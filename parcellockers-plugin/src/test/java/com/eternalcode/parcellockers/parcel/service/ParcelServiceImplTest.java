package com.eternalcode.parcellockers.parcel.service;

import static com.eternalcode.parcellockers.util.InventoryUtil.canHold;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.eternalcode.parcellockers.util.InventoryUtil;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ParcelServiceImplTest {

    private static final double FEE = 10.0;

    private final NoticeService noticeService = mock(NoticeService.class, RETURNS_DEEP_STUBS);
    private final ParcelRepository parcelRepository = mock(ParcelRepository.class);
    private final ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final Economy economy = mock(Economy.class);
    private final Server server = mock(Server.class);
    private final Player player = mock(Player.class);
    private final UUID playerId = UUID.randomUUID();
    private final Queue<Runnable> mainTasks = new ArrayDeque<>();
    private final ParcelServiceImpl service;

    ParcelServiceImplTest() {
        PluginConfig config = new PluginConfig();
        config.settings.smallParcelFee = FEE;

        when(this.server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(this.player.getUniqueId()).thenReturn(this.playerId);
        doAnswer(invocation -> this.mainTasks.add(invocation.getArgument(0)))
            .when(this.scheduler).run(any(Runnable.class));

        this.service = new ParcelServiceImpl(
            this.noticeService, this.parcelRepository, this.contentRepository,
            this.scheduler, config, this.economy, this.server);
    }

    @Test
    void duplicateParcelUuidFailsBeforeChargeAndDoesNotDeleteExistingParcel() {
        Parcel parcel = this.parcel(ParcelStatus.SENT);
        when(this.parcelRepository.saveIfAbsent(parcel)).thenReturn(CompletableFuture.completedFuture(false));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.service.send(this.player, parcel, List.of(mock(ItemStack.class))).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(this.economy, never()).withdrawPlayer(any(Player.class), anyDouble());
        verify(this.contentRepository, never()).save(any());
        verify(this.parcelRepository, never()).delete(parcel.uuid());
    }

    @Test
    void insufficientFundsRemovesInsertedParcelWithoutSavingContent() {
        Parcel parcel = this.parcel(ParcelStatus.SENT);
        when(this.parcelRepository.saveIfAbsent(parcel)).thenReturn(CompletableFuture.completedFuture(true));
        when(this.economy.withdrawPlayer(this.player, FEE)).thenReturn(response(EconomyResponse.ResponseType.FAILURE));
        when(this.parcelRepository.delete(parcel.uuid())).thenReturn(CompletableFuture.completedFuture(true));

        assertFalse(this.service.send(this.player, parcel, List.of(mock(ItemStack.class))).join());

        verify(this.parcelRepository).delete(parcel.uuid());
        verify(this.contentRepository, never()).save(any());
    }

    @Test
    void contentPersistenceFailureDeletesParcelAndRefundsFee() {
        Parcel parcel = this.parcel(ParcelStatus.SENT);
        IllegalStateException contentSaveFailure = new IllegalStateException("content save failed");
        when(this.parcelRepository.saveIfAbsent(parcel)).thenReturn(CompletableFuture.completedFuture(true));
        when(this.economy.withdrawPlayer(this.player, FEE)).thenReturn(response(EconomyResponse.ResponseType.SUCCESS));
        when(this.contentRepository.save(any())).thenReturn(CompletableFuture.failedFuture(contentSaveFailure));
        when(this.parcelRepository.delete(parcel.uuid())).thenReturn(CompletableFuture.completedFuture(true));

        CompletionException exception = assertThrows(CompletionException.class,
            () -> this.service.send(this.player, parcel, List.of(mock(ItemStack.class))).join());

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(contentSaveFailure, operationException.getCause());
        verify(this.parcelRepository).delete(parcel.uuid());
        verify(this.economy).depositPlayer(this.player, FEE);
    }

    @Test
    void collectCompletesOnlyAfterItemsWereGivenOnMainThread() {
        Parcel parcel = this.parcel(ParcelStatus.DELIVERED);
        ItemStack item = mock(ItemStack.class);
        this.stubContent(parcel, item);
        when(this.parcelRepository.commitCollection(eq(parcel.uuid()), eq(this.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        try (MockedStatic<InventoryUtil> inventory = mockStatic(InventoryUtil.class);
             MockedStatic<ItemUtil> itemUtil = mockStatic(ItemUtil.class)) {
            inventory.when(() -> canHold(this.player, List.of(item))).thenReturn(true);

            CompletableFuture<Void> result = this.service.collect(this.player, parcel);

            this.runNextMainTask();
            assertFalse(result.isDone());
            this.runNextMainTask();
            itemUtil.verify(() -> ItemUtil.giveItem(this.player, item));
            assertTrue(result.isDone());
        }
    }

    @Test
    void collectDoesNotGiveItemsWhenCollectionWasNotCommitted() {
        Parcel parcel = this.parcel(ParcelStatus.DELIVERED);
        ItemStack item = mock(ItemStack.class);
        this.stubContent(parcel, item);
        when(this.parcelRepository.commitCollection(eq(parcel.uuid()), eq(this.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(false));

        try (MockedStatic<InventoryUtil> inventory = mockStatic(InventoryUtil.class);
             MockedStatic<ItemUtil> itemUtil = mockStatic(ItemUtil.class)) {
            inventory.when(() -> canHold(this.player, List.of(item))).thenReturn(true);

            CompletableFuture<Void> result = this.service.collect(this.player, parcel);
            this.runNextMainTask();

            assertTrue(result.isDone());
            assertTrue(this.mainTasks.isEmpty());
            itemUtil.verifyNoInteractions();
        }
    }

    @Test
    void collectPersistenceFailureCompletesExceptionally() {
        Parcel parcel = this.parcel(ParcelStatus.DELIVERED);
        IllegalStateException databaseFailure = new IllegalStateException("database failed");
        this.stubContent(parcel);
        when(this.parcelRepository.commitCollection(eq(parcel.uuid()), eq(this.playerId), any()))
            .thenReturn(CompletableFuture.failedFuture(databaseFailure));

        try (MockedStatic<InventoryUtil> inventory = mockStatic(InventoryUtil.class)) {
            inventory.when(() -> canHold(this.player, List.of())).thenReturn(true);

            CompletableFuture<Void> result = this.service.collect(this.player, parcel);
            this.runNextMainTask();

            CompletionException exception = assertThrows(CompletionException.class, result::join);
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        }
    }

    private void stubContent(Parcel parcel, ItemStack... items) {
        ParcelContent content = new ParcelContent(parcel.uuid(), List.of(items));
        when(this.contentRepository.find(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
    }

    private void runNextMainTask() {
        this.mainTasks.remove().run();
    }

    private Parcel parcel(ParcelStatus status) {
        return new Parcel(UUID.randomUUID(), this.playerId, "name", null, false, this.playerId,
            ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), status);
    }

    private static EconomyResponse response(EconomyResponse.ResponseType type) {
        return new EconomyResponse(FEE, 100.0, type, null);
    }
}
