package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageReservation;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelDispatchServiceTest {

    private final LockerManager lockerManager = mock(LockerManager.class);
    private final PluginParcelService parcelService = mock(PluginParcelService.class);
    private final DeliveryManager deliveryManager = mock(DeliveryManager.class);
    private final ItemStorageManager itemStorageManager = mock(ItemStorageManager.class);
    private final ItemStorageReservation reservation = mock(ItemStorageReservation.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final PluginConfig config = new PluginConfig();
    private final Player sender = mock(Player.class);
    private final Parcel parcel = parcel();
    private final List<ItemStack> items = List.of(mock(ItemStack.class));
    private final ParcelDispatchService dispatcher = new ParcelDispatchService(
        this.lockerManager,
        this.parcelService,
        this.deliveryManager,
        this.itemStorageManager,
        this.scheduler,
        this.config,
        this.noticeService
    );

    @BeforeEach
    void setUp() {
        this.config.settings.parcelSendDuration = Duration.ofMinutes(30);
        when(this.sender.getUniqueId()).thenReturn(this.parcel.sender());
        when(this.itemStorageManager.reserve(this.parcel.sender()))
            .thenReturn(Optional.of(this.reservation));
    }

    @Test
    void successCompletesOnlyAfterDeliveryIsPersistedAndTaskIsScheduled() {
        CompletableFuture<Delivery> deliveryCreated = new CompletableFuture<>();
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete())
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(deliveryCreated);

        CompletableFuture<Boolean> result = this.dispatcher.dispatch(this.sender, this.parcel, this.items);

        assertFalse(result.isDone());
        verify(this.scheduler, never()).runLaterAsync(any(), any());

        deliveryCreated.complete(mock(Delivery.class));

        assertTrue(result.join());
        verify(this.scheduler).runLaterAsync(any(), eq(this.config.settings.parcelSendDuration));
        verify(this.reservation).close();
    }

    @Test
    void fullLockerCompletesFalseWithoutPersistingParcel() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(true));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.parcelService, never()).send(any(), any(), any());
        verify(this.reservation).close();
    }

    @Test
    void fullnessLookupFailureCompletesExceptionally() {
        IllegalStateException lookupFailure = new IllegalStateException("lookup failed");
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.failedFuture(lookupFailure));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(lookupFailure, exception.getCause().getCause());
        verify(this.parcelService, never()).send(any(), any(), any());
    }

    @Test
    void cancelledPersistenceStepCompletesFalseWithoutContinuing() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(false));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.itemStorageManager, never()).delete(any());
    }

    @Test
    void parcelPersistenceFailureCompletesExceptionally() {
        IllegalStateException persistenceFailure =
            new IllegalStateException("persistence failed");
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.failedFuture(persistenceFailure));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(persistenceFailure, exception.getCause().getCause());
        verify(this.reservation, never()).delete();
    }

    @Test
    void storageDeleteFailureRollsBackAndCompletesExceptionally() {
        IllegalStateException deleteFailure = new IllegalStateException("delete failed");
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete())
            .thenReturn(CompletableFuture.failedFuture(deleteFailure));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(deleteFailure, exception.getCause().getCause());
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.deliveryManager, never()).create(any(), any());
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }

    @Test
    void synchronousStorageDeleteFailureStillRollsBack() {
        IllegalStateException deleteFailure = new IllegalStateException("delete failed");
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete()).thenThrow(deleteFailure);
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(deleteFailure, operationException.getCause());
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.deliveryManager, never()).create(any(), any());
    }

    @Test
    void synchronousDeliveryCreateFailureRollsBackAndCompletesExceptionally() {
        IllegalStateException createFailure = new IllegalStateException("create failed");
        CompletableFuture<ItemStorage> restored = new CompletableFuture<>();
        CompletableFuture<Void> rolledBack = new CompletableFuture<>();
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete())
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenThrow(createFailure);
        when(this.reservation.restore(this.items))
            .thenReturn(restored);
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(rolledBack);

        CompletableFuture<Boolean> result = this.dispatcher.dispatch(this.sender, this.parcel, this.items);

        assertFalse(result.isDone());
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);

        restored.complete(mock(ItemStorage.class));

        assertFalse(result.isDone());
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);

        rolledBack.complete(null);
        CompletionException exception =
            assertThrows(CompletionException.class, result::join);
        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(createFailure, exception.getCause().getCause());
        verify(this.reservation).restore(this.items);
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }

    @Test
    void asynchronousDeliveryCreateFailureRollsBackAndCompletesExceptionally() {
        IllegalStateException createFailure = new IllegalStateException("create failed");
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete())
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(createFailure));
        when(this.reservation.restore(this.items))
            .thenReturn(CompletableFuture.completedFuture(mock(ItemStorage.class)));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(createFailure, exception.getCause().getCause());
        verify(this.reservation).restore(this.items);
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.scheduler, never()).runLaterAsync(any(), any());
        verify(this.noticeService, times(1)).player(eq(this.parcel.sender()), any());
    }

    @Test
    void schedulingFailureDeletesDeliveryThenRestoresStorageThenRollsBackExceptionally() {
        IllegalStateException schedulingFailure = new IllegalStateException("schedule failed");
        CompletableFuture<Boolean> deliveryDeleted = new CompletableFuture<>();
        CompletableFuture<ItemStorage> restored = new CompletableFuture<>();
        CompletableFuture<Void> rolledBack = new CompletableFuture<>();
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(Delivery.class)));
        when(this.scheduler.runLaterAsync(any(), any()))
            .thenThrow(schedulingFailure);
        when(this.deliveryManager.delete(this.parcel.uuid())).thenReturn(deliveryDeleted);
        when(this.reservation.restore(this.items)).thenReturn(restored);
        when(this.parcelService.rollbackSend(this.sender, this.parcel)).thenReturn(rolledBack);

        CompletableFuture<Boolean> result = this.dispatcher.dispatch(this.sender, this.parcel, this.items);

        assertFalse(result.isDone());
        verify(this.deliveryManager).delete(this.parcel.uuid());
        verify(this.reservation, never()).restore(this.items);

        deliveryDeleted.complete(true);
        verify(this.reservation).restore(this.items);
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);

        restored.complete(mock(ItemStorage.class));
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        assertFalse(result.isDone());

        rolledBack.complete(null);
        CompletionException exception =
            assertThrows(CompletionException.class, result::join);
        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(schedulingFailure, exception.getCause().getCause());
    }

    @Test
    void restoreFailureCompletesExceptionallyInsteadOfFalse() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(ParcelDispatchService.class.getName());
        logger.addHandler(handler);
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("create failed")));
        when(this.reservation.restore(this.items))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("restore failed")));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        CompletionException exception;
        try {
            exception = assertThrows(CompletionException.class,
                () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());
        } finally {
            logger.removeHandler(handler);
        }

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.noticeService, times(1)).player(eq(this.parcel.sender()), any());
        verify(this.reservation).close();
        assertTrue(records.stream().anyMatch(record ->
            record.getMessage().contains(this.parcel.uuid().toString())
                && record.getMessage().contains(this.parcel.sender().toString())
                && record.getThrown() == exception.getCause()));
    }

    @Test
    void deliveryDeleteFailureCompletesExceptionallyInsteadOfFalse() {
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(Delivery.class)));
        when(this.scheduler.runLaterAsync(any(), any()))
            .thenThrow(new IllegalStateException("schedule failed"));
        when(this.deliveryManager.delete(this.parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.reservation.restore(this.items))
            .thenReturn(CompletableFuture.completedFuture(mock(ItemStorage.class)));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(this.reservation).restore(this.items);
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
    }

    @Test
    void schedulingCompensationAttemptsEveryStepAndAggregatesCleanupFailures() {
        IllegalStateException schedulingFailure = new IllegalStateException("schedule failed");
        IllegalStateException deliveryCleanupFailure =
            new IllegalStateException("delivery cleanup failed");
        IllegalStateException restoreFailure = new IllegalStateException("restore failed");
        IllegalStateException rollbackFailure = new IllegalStateException("rollback failed");
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(Delivery.class)));
        when(this.scheduler.runLaterAsync(any(), any())).thenThrow(schedulingFailure);
        when(this.deliveryManager.delete(this.parcel.uuid()))
            .thenReturn(CompletableFuture.failedFuture(deliveryCleanupFailure));
        when(this.reservation.restore(this.items))
            .thenReturn(CompletableFuture.failedFuture(restoreFailure));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.failedFuture(rollbackFailure));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(schedulingFailure, operationException.getCause());
        assertEquals(
            List.of(deliveryCleanupFailure, restoreFailure, rollbackFailure),
            List.of(operationException.getSuppressed())
        );
        verify(this.deliveryManager).delete(this.parcel.uuid());
        verify(this.reservation).restore(this.items);
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
    }

    @Test
    void rollbackFailureCompletesExceptionallyInsteadOfFalse() {
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("create failed")));
        when(this.reservation.restore(this.items))
            .thenReturn(CompletableFuture.completedFuture(mock(ItemStorage.class)));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("rollback failed")));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
    }

    @Test
    void successNoticeFailureDoesNotCompensateCommittedSend() {
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(Delivery.class)));
        doThrow(new IllegalStateException("notice failed"))
            .when(this.noticeService).player(eq(this.parcel.sender()), any());

        assertTrue(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.deliveryManager, never()).delete(this.parcel.uuid());
        verify(this.reservation, never()).restore(this.items);
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);
    }

    private void stubSuccessfulStart() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.reservation.delete())
            .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    void secondDispatchBySameSenderToDifferentLockerIsRejectedWhileFirstIsActive() {
        Parcel secondParcel = new Parcel(
            UUID.randomUUID(),
            this.parcel.sender(),
            "second",
            null,
            false,
            UUID.randomUUID(),
            ParcelSize.SMALL,
            UUID.randomUUID(),
            UUID.randomUUID(),
            ParcelStatus.SENT
        );
        CompletableFuture<Boolean> firstLockerFull = new CompletableFuture<>();
        when(this.itemStorageManager.reserve(this.parcel.sender()))
            .thenReturn(Optional.of(this.reservation), Optional.empty());
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(firstLockerFull);

        CompletableFuture<Boolean> first = this.dispatcher.dispatch(this.sender, this.parcel, this.items);
        CompletableFuture<Boolean> second = this.dispatcher.dispatch(this.sender, secondParcel, this.items);

        assertFalse(second.join());
        verify(this.lockerManager, never()).isLockerFull(secondParcel.destinationLocker());

        firstLockerFull.complete(true);
        assertFalse(first.join());
        verify(this.reservation).close();
    }

    @Test
    void synchronousFailureBeforeLockerChainRegistrationReleasesReservation() {
        Parcel brokenParcel = mock(Parcel.class);
        IllegalStateException failure = new IllegalStateException("destination unavailable");
        when(brokenParcel.destinationLocker()).thenThrow(failure);

        CompletableFuture<Boolean> result =
            this.dispatcher.dispatch(this.sender, brokenParcel, this.items);

        CompletionException exception =
            assertThrows(CompletionException.class, result::join);
        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(failure, operationException.getCause());
        verify(this.reservation).close();
        verify(this.lockerManager, never()).isLockerFull(any());
    }

    private static Parcel parcel() {
        return new Parcel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "name",
            null,
            false,
            UUID.randomUUID(),
            ParcelSize.SMALL,
            UUID.randomUUID(),
            UUID.randomUUID(),
            ParcelStatus.SENT
        );
    }
}
