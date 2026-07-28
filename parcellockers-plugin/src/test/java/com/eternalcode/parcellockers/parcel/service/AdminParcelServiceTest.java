package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AdminParcelServiceTest {

    @Test
    void statusChangeUsesAuthoritativeStateInsteadOfStaleCollectedSnapshot() {
        Parcel sent = parcel(ParcelStatus.SENT, false);
        Parcel staleCollected = withStatus(sent, ParcelStatus.COLLECTED);
        Parcel delivered = withStatus(sent, ParcelStatus.DELIVERED);
        PluginParcelService parcelService = parcelServiceWithAuthoritative(sent);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        runSerializedOperationsImmediately(
            parcelService, sent.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        AdminParcelService service = service(
            parcelService, deliveryManager, mock(Scheduler.class));

        EditResult result =
            service.changeStatus(staleCollected, ParcelStatus.DELIVERED).join();

        assertEquals(EditResult.Status.OK, result.status());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            delivered, ParcelStatus.SENT);
    }

    @Test
    void fieldEditMergesOnlyChangedFieldIntoAuthoritativeCollectedParcel() {
        Parcel stale = parcel(ParcelStatus.SENT, false);
        Parcel authoritative = new Parcel(
            stale.uuid(), UUID.randomUUID(), "current-name", "current-description", true,
            UUID.randomUUID(), ParcelSize.LARGE, UUID.randomUUID(), UUID.randomUUID(),
            ParcelStatus.COLLECTED);
        Parcel expected = new Parcel(
            authoritative.uuid(), authoritative.sender(), "edited-name",
            authoritative.description(), authoritative.priority(), authoritative.receiver(),
            authoritative.size(), authoritative.entryLocker(),
            authoritative.destinationLocker(), ParcelStatus.COLLECTED);
        PluginParcelService parcelService = mock(PluginParcelService.class);
        runSerializedOperationsImmediately(parcelService, stale.uuid(), new AtomicBoolean());
        when(parcelService.getAuthoritativeWithinParcelOperation(stale.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(authoritative)));
        when(parcelService.updateIfStatusWithinParcelOperation(
            expected, ParcelStatus.COLLECTED))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(parcelService.update(any())).thenReturn(CompletableFuture.completedFuture(null));
        AdminParcelService service = service(
            parcelService, mock(DeliveryManager.class), mock(Scheduler.class));

        EditResult result = service.changeName(stale, "edited-name").join();

        assertEquals(EditResult.Status.OK, result.status());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            expected, ParcelStatus.COLLECTED);
        verify(parcelService, never()).update(any());
    }

    @Test
    void statusChangeRollsBackParcelAndCreatedDeliveryWhenSchedulingIsRejected() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED, false);
        Parcel sent = withStatus(delivered, ParcelStatus.SENT);
        PluginParcelService parcelService = parcelServiceWithAuthoritative(delivered);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(
            parcelService, delivered.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(delivered.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(deliveryManager.create(eq(delivered.uuid()), any()))
            .thenReturn(CompletableFuture.completedFuture(
                new Delivery(delivered.uuid(), Instant.now())));
        when(deliveryManager.delete(delivered.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        doThrow(new IllegalStateException("scheduler rejected"))
            .when(scheduler).runLaterAsync(any(), any());
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletionException failure = assertThrows(
            CompletionException.class,
            () -> service.changeStatus(delivered, ParcelStatus.SENT).join());

        assertInstanceOf(IllegalStateException.class, failure.getCause());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            sent, ParcelStatus.DELIVERED);
        verify(parcelService).updateIfStatusWithinParcelOperation(
            delivered, ParcelStatus.SENT);
        verify(deliveryManager).delete(delivered.uuid());
    }

    @Test
    void statusChangeCompensatesSynchronousDeliveryCreateFailure() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED, false);
        Parcel sent = withStatus(delivered, ParcelStatus.SENT);
        PluginParcelService parcelService = parcelServiceWithAuthoritative(delivered);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(
            parcelService, delivered.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(delivered.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        doThrow(new IllegalStateException("delivery create threw"))
            .when(deliveryManager).create(eq(delivered.uuid()), any());
        when(deliveryManager.delete(delivered.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletionException failure = assertThrows(
            CompletionException.class,
            () -> service.changeStatus(delivered, ParcelStatus.SENT).join());

        assertEquals("delivery create threw", failure.getCause().getMessage());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            sent, ParcelStatus.DELIVERED);
        verify(parcelService).updateIfStatusWithinParcelOperation(
            delivered, ParcelStatus.SENT);
        verify(deliveryManager).delete(delivered.uuid());
    }

    @Test
    void statusChangeRestoresSentParcelDeliveryAndTaskWhenDeleteFails() {
        Parcel sent = parcel(ParcelStatus.SENT, false);
        Parcel delivered = withStatus(sent, ParcelStatus.DELIVERED);
        Delivery priorDelivery =
            new Delivery(sent.uuid(), Instant.now().plus(Duration.ofMinutes(5)));
        PluginParcelService parcelService = parcelServiceWithAuthoritative(sent);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(parcelService, sent.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(priorDelivery)));
        when(deliveryManager.delete(sent.uuid()))
            .thenReturn(CompletableFuture.failedFuture(
                new IllegalStateException("delivery delete failed")));
        when(deliveryManager.update(sent.uuid(), priorDelivery.deliveryTimestamp()))
            .thenReturn(CompletableFuture.completedFuture(priorDelivery));
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        assertThrows(
            CompletionException.class,
            () -> service.changeStatus(sent, ParcelStatus.DELIVERED).join());

        verify(parcelService).updateIfStatusWithinParcelOperation(
            delivered, ParcelStatus.SENT);
        verify(parcelService).updateIfStatusWithinParcelOperation(
            sent, ParcelStatus.DELIVERED);
        verify(deliveryManager).update(sent.uuid(), priorDelivery.deliveryTimestamp());
        verify(scheduler).runLaterAsync(any(), any());
    }

    @Test
    void statusChangeCompensatesWhenDeliveryDeleteReturnsFalse() {
        Parcel sent = parcel(ParcelStatus.SENT, false);
        Parcel delivered = withStatus(sent, ParcelStatus.DELIVERED);
        Delivery priorDelivery =
            new Delivery(sent.uuid(), Instant.now().plus(Duration.ofMinutes(5)));
        PluginParcelService parcelService = parcelServiceWithAuthoritative(sent);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(
            parcelService, sent.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(priorDelivery)));
        when(deliveryManager.delete(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(deliveryManager.update(sent.uuid(), priorDelivery.deliveryTimestamp()))
            .thenReturn(CompletableFuture.completedFuture(priorDelivery));
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletionException failure = assertThrows(
            CompletionException.class,
            () -> service.changeStatus(sent, ParcelStatus.DELIVERED).join());

        assertEquals("Delivery delete returned false", failure.getCause().getMessage());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            delivered, ParcelStatus.SENT);
        verify(parcelService).updateIfStatusWithinParcelOperation(
            sent, ParcelStatus.DELIVERED);
        verify(deliveryManager).update(sent.uuid(), priorDelivery.deliveryTimestamp());
        verify(scheduler).runLaterAsync(any(), any());
    }

    @Test
    void priorityChangeRestoresParcelDeliveryAndTaskWhenDeliveryUpdateFails() {
        Parcel normal = parcel(ParcelStatus.SENT, false);
        Parcel priority = withPriority(normal, true);
        Delivery priorDelivery =
            new Delivery(normal.uuid(), Instant.now().plus(Duration.ofMinutes(5)));
        PluginParcelService parcelService = parcelServiceWithAuthoritative(normal);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(parcelService, normal.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(parcelService.updateWithinParcelOperation(normal))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(deliveryManager.get(normal.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(priorDelivery)));
        when(deliveryManager.update(eq(normal.uuid()), any()))
            .thenReturn(
                CompletableFuture.failedFuture(
                    new IllegalStateException("delivery update failed")),
                CompletableFuture.completedFuture(priorDelivery));
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        assertThrows(
            CompletionException.class,
            () -> service.changePriority(normal, true).join());

        verify(parcelService).updateIfStatusWithinParcelOperation(
            priority, ParcelStatus.SENT);
        verify(parcelService).updateWithinParcelOperation(normal);
        verify(deliveryManager, times(2)).update(eq(normal.uuid()), any());
        verify(scheduler).runLaterAsync(any(), any());
    }

    @Test
    void compensationAggregatesParcelAndDeliveryRestoreFailures() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED, false);
        PluginParcelService parcelService = parcelServiceWithAuthoritative(delivered);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        runSerializedOperationsImmediately(
            parcelService, delivered.uuid(), new AtomicBoolean());
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(
                CompletableFuture.completedFuture(true),
                CompletableFuture.failedFuture(
                    new IllegalStateException("parcel rollback failed")));
        when(deliveryManager.get(delivered.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(deliveryManager.create(eq(delivered.uuid()), any()))
            .thenReturn(CompletableFuture.failedFuture(
                new IllegalStateException("delivery create failed")));
        when(deliveryManager.delete(delivered.uuid()))
            .thenReturn(CompletableFuture.failedFuture(
                new IllegalStateException("delivery rollback failed")));
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletionException failure = assertThrows(
            CompletionException.class,
            () -> service.changeStatus(delivered, ParcelStatus.SENT).join());

        assertEquals("delivery create failed", failure.getCause().getMessage());
        assertEquals(2, failure.getCause().getSuppressed().length);
    }

    @Test
    void changeStatusKeepsParcelGateUntilDeliveryIsArmed() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED, false);
        AtomicBoolean gateHeld = new AtomicBoolean();
        PluginParcelService parcelService = parcelServiceWithWithinStatusUpdate(delivered);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        CompletableFuture<Delivery> deliveryUpdated = new CompletableFuture<>();

        runSerializedOperationsImmediately(parcelService, delivered.uuid(), gateHeld);
        when(deliveryManager.get(delivered.uuid())).thenAnswer(invocation -> {
            assertTrue(gateHeld.get(), "delivery read must run while the parcel gate is held");
            return CompletableFuture.completedFuture(Optional.empty());
        });
        when(deliveryManager.create(eq(delivered.uuid()), any())).thenAnswer(invocation -> {
            assertTrue(gateHeld.get(), "delivery write must run while the parcel gate is held");
            return deliveryUpdated;
        });
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletableFuture<EditResult> result =
            service.changeStatus(delivered, ParcelStatus.SENT);

        assertFalse(result.isDone(), "the gate must cover the pending delivery write");
        assertTrue(gateHeld.get());

        deliveryUpdated.complete(new Delivery(delivered.uuid(), Instant.now()));

        assertEquals(EditResult.Status.OK, result.join().status());
        assertFalse(gateHeld.get());
        verify(parcelService).serializeParcelOperation(eq(delivered.uuid()), any());
        verify(parcelService, never()).updateIfStatus(any(), any());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            any(), eq(ParcelStatus.DELIVERED));
    }

    @Test
    void changePriorityKeepsParcelGateUntilDeliveryAndSchedulingComplete() {
        Parcel sent = parcel(ParcelStatus.SENT, false);
        AtomicBoolean gateHeld = new AtomicBoolean();
        PluginParcelService parcelService = mock(PluginParcelService.class);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        CompletableFuture<Delivery> deliveryUpdated = new CompletableFuture<>();
        Delivery current = new Delivery(sent.uuid(), Instant.now().plus(Duration.ofMinutes(10)));

        runSerializedOperationsImmediately(parcelService, sent.uuid(), gateHeld);
        when(parcelService.getAuthoritativeWithinParcelOperation(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(sent)));
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.get(sent.uuid())).thenAnswer(invocation -> {
            assertTrue(gateHeld.get(), "delivery read must run while the parcel gate is held");
            return CompletableFuture.completedFuture(Optional.of(current));
        });
        when(deliveryManager.update(eq(sent.uuid()), any())).thenAnswer(invocation -> {
            assertTrue(gateHeld.get(), "delivery write must run while the parcel gate is held");
            return deliveryUpdated;
        });
        AdminParcelService service = service(parcelService, deliveryManager, scheduler);

        CompletableFuture<EditResult> result = service.changePriority(sent, true);

        assertFalse(result.isDone(), "the gate must cover the pending delivery write");
        assertTrue(gateHeld.get());

        deliveryUpdated.complete(new Delivery(sent.uuid(), Instant.now()));

        assertEquals(EditResult.Status.OK, result.join().status());
        assertFalse(gateHeld.get());
        verify(scheduler).runLaterAsync(any(), any());
        verify(parcelService).serializeParcelOperation(eq(sent.uuid()), any());
        verify(parcelService, never()).update(any());
        verify(parcelService).updateIfStatusWithinParcelOperation(
            any(), eq(ParcelStatus.SENT));
    }

    @Test
    void capacityMatchesContentGuiUsableSlots() {
        assertEquals(9, AdminParcelService.capacity(ParcelSize.SMALL));
        assertEquals(18, AdminParcelService.capacity(ParcelSize.MEDIUM));
        assertEquals(27, AdminParcelService.capacity(ParcelSize.LARGE));
    }

    @Test
    void enablingPriorityShortensDeliveryByDelta() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(5)); // normal delivery scheduled in 5 min
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, false, true, normal, priority, now);

        // delta = priority - normal = -4 min; oldTs - 4 min = now + 1 min
        assertEquals(now.plus(Duration.ofMinutes(1)), shifted);
    }

    @Test
    void disablingPriorityExtendsDeliveryByDelta() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(1));
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, true, false, normal, priority, now);

        // delta = normal - priority = +4 min; oldTs + 4 min = now + 5 min
        assertEquals(now.plus(Duration.ofMinutes(5)), shifted);
    }

    @Test
    void overdueShiftIsClampedToNow() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofSeconds(30)); // 30s left
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        // enabling priority: delta = -4 min, oldTs - 4 min is in the past -> clamp to now
        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, false, true, normal, priority, now);

        assertEquals(now, shifted);
    }

    @Test
    void unchangedPriorityKeepsTimestamp() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(3));
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, true, true, normal, priority, now);

        assertEquals(oldTs, shifted);
    }

    private static PluginParcelService parcelServiceWithWithinStatusUpdate(Parcel parcel) {
        PluginParcelService parcelService = mock(PluginParcelService.class);
        when(parcelService.getAuthoritativeWithinParcelOperation(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(parcel)));
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        return parcelService;
    }

    private static PluginParcelService parcelServiceWithAuthoritative(Parcel parcel) {
        PluginParcelService parcelService = mock(PluginParcelService.class);
        when(parcelService.getAuthoritativeWithinParcelOperation(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(parcel)));
        return parcelService;
    }

    private static void runSerializedOperationsImmediately(
        PluginParcelService parcelService,
        UUID parcelId,
        AtomicBoolean gateHeld
    ) {
        when(parcelService.serializeParcelOperation(eq(parcelId), any()))
            .thenAnswer(invocation -> {
                gateHeld.set(true);
                @SuppressWarnings("unchecked")
                Supplier<CompletableFuture<EditResult>> operation = invocation.getArgument(1);
                CompletableFuture<EditResult> result = operation.get();
                return result.whenComplete((ignored, throwable) -> gateHeld.set(false));
            });
    }

    private static AdminParcelService service(
        PluginParcelService parcelService,
        DeliveryManager deliveryManager,
        Scheduler scheduler
    ) {
        return new AdminParcelService(
            parcelService, null, deliveryManager, null, new PluginConfig(), scheduler);
    }

    private static Parcel parcel(ParcelStatus status, boolean priority) {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "name", "description", priority,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), status);
    }

    private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
        return new Parcel(
            parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(), parcel.priority(),
            parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), status);
    }

    private static Parcel withPriority(Parcel parcel, boolean priority) {
        return new Parcel(
            parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(), priority,
            parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), parcel.status());
    }
}
