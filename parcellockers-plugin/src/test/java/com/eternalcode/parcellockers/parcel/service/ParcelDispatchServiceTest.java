package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ParcelDispatchServiceTest {

    private final LockerManager lockerManager = mock(LockerManager.class);
    private final PluginParcelService parcelService = mock(PluginParcelService.class);
    private final DeliveryManager deliveryManager = mock(DeliveryManager.class);
    private final ItemStorageManager itemStorageManager = mock(ItemStorageManager.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final PluginConfig config = new PluginConfig();
    private final Player sender = mock(Player.class);
    private final Parcel parcel = new Parcel(UUID.randomUUID(), UUID.randomUUID(), "name", null, false,
        UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.SENT);
    private final List<ItemStack> items = List.of(mock(ItemStack.class));
    private final ParcelDispatchService dispatcher = new ParcelDispatchService(
        this.lockerManager,
        this.parcelService,
        this.deliveryManager,
        this.itemStorageManager,
        this.scheduler,
        this.config,
        this.noticeService,
        Logger.getLogger(ParcelDispatchServiceTest.class.getName())
    );

    @BeforeEach
    void setUp() {
        this.config.settings.parcelSendDuration = Duration.ofMinutes(30);
        when(this.sender.getUniqueId()).thenReturn(this.parcel.sender());
        when(this.lockerManager.isLockerFull(this.parcel.destinationLocker()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(this.parcelService.rollbackSend(this.sender, this.parcel))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    void successCompletesOnlyAfterDeliveryIsPersistedAndTaskIsScheduled() {
        CompletableFuture<Delivery> deliveryCreated = new CompletableFuture<>();
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class))).thenReturn(deliveryCreated);

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
    void refusedSendCompletesFalseWithoutTouchingStorage() {
        when(this.parcelService.send(this.sender, this.parcel, this.items))
            .thenReturn(CompletableFuture.completedFuture(false));

        assertFalse(this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        verify(this.itemStorageManager, never()).delete(any());
    }

    @Test
    void storageDeleteFailureRollsBackAndCompletesExceptionally() {
        when(this.itemStorageManager.delete(this.parcel.sender()))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("delete failed")));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.deliveryManager, never()).create(any(), any());
    }

    @Test
    void deliveryCreateFailureRestoresStorageThenRollsBack() {
        when(this.deliveryManager.create(eq(this.parcel.uuid()), any(Instant.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("delivery failed")));
        when(this.itemStorageManager.create(this.parcel.sender(), this.items))
            .thenReturn(CompletableFuture.completedFuture(new ItemStorage(this.parcel.sender(), this.items)));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.dispatcher.dispatch(this.sender, this.parcel, this.items).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        InOrder order = inOrder(this.itemStorageManager, this.parcelService);
        order.verify(this.itemStorageManager).create(this.parcel.sender(), this.items);
        order.verify(this.parcelService).rollbackSend(this.sender, this.parcel);
        verify(this.scheduler, never()).runLaterAsync(any(), any());
    }
}
