package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AdminParcelServiceTest {

    private static Parcel collectedParcel() {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "name", "description", false,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED);
    }

    @Test
    void changeStatusRefusesCollectedParcelWithoutTouchingAnyCollaborator() {
        // No collaborator is a usable mock/instance here on purpose: the COLLECTED guard must
        // short-circuit before dereferencing any of them, so a regression that removes the guard
        // and reaches a collaborator call would fail this test with an NPE instead of passing.
        AdminParcelService service = new AdminParcelService(null, null, null, null, null, null);

        Parcel collected = collectedParcel();
        EditResult result = service.changeStatus(collected, ParcelStatus.SENT).join();

        assertEquals(EditResult.Status.PARCEL_COLLECTED, result.status());
    }

    @Test
    void changeStatusKeepsParcelGateUntilDeliveryIsArmed() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED, false);
        AtomicBoolean gateHeld = new AtomicBoolean();
        PluginParcelService parcelService = parcelServiceWithWithinStatusUpdate();
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        Scheduler scheduler = mock(Scheduler.class);
        CompletableFuture<Delivery> deliveryUpdated = new CompletableFuture<>();

        runSerializedOperationsImmediately(parcelService, delivered.uuid(), gateHeld);
        when(deliveryManager.get(delivered.uuid())).thenAnswer(invocation -> {
            assertTrue(gateHeld.get(), "delivery read must run while the parcel gate is held");
            return CompletableFuture.completedFuture(Optional.empty());
        });
        when(deliveryManager.update(eq(delivered.uuid()), any())).thenAnswer(invocation -> {
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
        when(parcelService.updateWithinParcelOperation(any()))
            .thenReturn(CompletableFuture.completedFuture(null));
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
        verify(parcelService).updateWithinParcelOperation(any());
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

    private static PluginParcelService parcelServiceWithWithinStatusUpdate() {
        PluginParcelService parcelService = mock(PluginParcelService.class);
        when(parcelService.updateIfStatusWithinParcelOperation(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
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
}
