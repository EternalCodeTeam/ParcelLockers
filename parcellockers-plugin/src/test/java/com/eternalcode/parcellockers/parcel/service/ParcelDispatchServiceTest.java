package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelDispatchServiceTest {

    private final LockerManager lockerManager = mock(LockerManager.class);
    private final PluginParcelService parcelService = mock(PluginParcelService.class);
    private final DeliveryManager deliveryManager = mock(DeliveryManager.class);
    private final ItemStorageManager itemStorageManager = mock(ItemStorageManager.class);
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
    }

    @Test
    void successCompletesOnlyAfterDeliveryIsPersistedAndTaskIsScheduled() {
        CompletableFuture<Delivery> deliveryCreated = new CompletableFuture<>();
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(deliveryCreated);

        CompletableFuture<Boolean> result = this.dispatcher.dispatch(this.sender, this.parcel, this.items);

        assertFalse(result.isDone());
        verify(this.scheduler, never()).runLaterAsync(any(), any());

        deliveryCreated.complete(mock(Delivery.class));

        assertTrue(result.join());
        verify(this.scheduler).runLaterAsync(any(), eq(this.config.settings.parcelSendDuration));
    }

    @Test
    void fullLockerCompletesFalseWithoutPersistingParcel() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(true));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

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
    void storageDeleteFailureRollsBackAndCompletesFalse() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("delete failed")));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.deliveryManager, never()).create(any(), any());
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }

    @Test
    void synchronousDeliveryCreateFailureRollsBackAndCompletesFalse() {
        CompletableFuture<ItemStorage> restored = new CompletableFuture<>();
        CompletableFuture<Void> rolledBack = new CompletableFuture<>();
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenThrow(new IllegalStateException("create failed"));
        when(this.itemStorageManager.create(this.parcel.sender(), this.items))
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

        assertFalse(result.join());
        verify(this.itemStorageManager).create(this.parcel.sender(), this.items);
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }

    @Test
    void asynchronousDeliveryCreateFailureRollsBackAndCompletesFalse() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("create failed")));
        when(this.itemStorageManager.create(this.parcel.sender(), this.items))
            .thenReturn(CompletableFuture.completedFuture(mock(ItemStorage.class)));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.itemStorageManager).create(this.parcel.sender(), this.items);
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }

    @Test
    void schedulingFailureDeletesDeliveryThenRestoresStorageThenRollsBack() {
        CompletableFuture<Boolean> deliveryDeleted = new CompletableFuture<>();
        CompletableFuture<ItemStorage> restored = new CompletableFuture<>();
        CompletableFuture<Void> rolledBack = new CompletableFuture<>();
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.completedFuture(mock(Delivery.class)));
        when(this.scheduler.runLaterAsync(any(), any()))
            .thenThrow(new IllegalStateException("schedule failed"));
        when(this.deliveryManager.delete(this.parcel.uuid())).thenReturn(deliveryDeleted);
        when(this.itemStorageManager.create(this.parcel.sender(), this.items)).thenReturn(restored);
        when(this.parcelService.rollbackSend(this.sender, this.parcel)).thenReturn(rolledBack);

        CompletableFuture<Boolean> result = this.dispatcher.dispatch(this.sender, this.parcel, this.items);

        assertFalse(result.isDone());
        verify(this.deliveryManager).delete(this.parcel.uuid());
        verify(this.itemStorageManager, never()).create(this.parcel.sender(), this.items);

        deliveryDeleted.complete(true);
        verify(this.itemStorageManager).create(this.parcel.sender(), this.items);
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);

        restored.complete(mock(ItemStorage.class));
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        assertFalse(result.isDone());

        rolledBack.complete(null);
        assertFalse(result.join());
    }

    @Test
    void restoreFailureCompletesExceptionallyInsteadOfFalse() {
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("create failed")));
        when(this.itemStorageManager.create(this.parcel.sender(), this.items))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("restore failed")));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);
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

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(this.itemStorageManager, never()).create(this.parcel.sender(), this.items);
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);
    }

    @Test
    void rollbackFailureCompletesExceptionallyInsteadOfFalse() {
        this.stubSuccessfulStart();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("create failed")));
        when(this.itemStorageManager.create(this.parcel.sender(), this.items))
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
        verify(this.itemStorageManager, never()).create(this.parcel.sender(), this.items);
        verify(this.parcelService, never()).rollbackSend(this.sender, this.parcel);
    }

    private void stubSuccessfulStart() {
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.completedFuture(true));
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
