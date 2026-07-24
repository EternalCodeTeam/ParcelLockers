package com.eternalcode.parcellockers.parcel.service;

import static com.eternalcode.parcellockers.util.InventoryUtil.canHold;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import com.eternalcode.parcellockers.parcel.event.ParcelCollectEvent;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ParcelServiceImplTest {

    @Test
    void collectRejectsForgedReceiverFromAuthoritativeRecordWithoutSideEffects() {
        CollectFixture fixture = new CollectFixture();
        Parcel forged = withReceiver(fixture.authoritative, fixture.playerId);
        when(fixture.parcelRepository.findById(forged.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(fixture.authoritative)));

        CompletionException exception = assertThrows(
            CompletionException.class, () -> fixture.service.collect(fixture.player, forged).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(fixture.pluginManager, never()).callEvent(any());
        verify(fixture.contentRepository, never()).find(any());
        verify(fixture.parcelRepository, never()).markCollected(any());
    }

    @Test
    void collectRejectsStaleDeliveredDtoWhenAuthoritativeRecordIsNotDelivered() {
        CollectFixture fixture = new CollectFixture();
        Parcel caller = withReceiver(fixture.authoritative, fixture.playerId);
        Parcel collected = withStatus(caller, ParcelStatus.COLLECTED);
        when(fixture.parcelRepository.findById(caller.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(collected)));

        CompletionException exception = assertThrows(
            CompletionException.class, () -> fixture.service.collect(fixture.player, caller).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(fixture.pluginManager, never()).callEvent(any());
        verify(fixture.contentRepository, never()).find(any());
        verify(fixture.parcelRepository, never()).markCollected(any());
    }

    @Test
    void collectPersistenceFailureCompletesExceptionallyEvenWhenFailureNoticeThrows() {
        CollectFixture fixture = new CollectFixture();
        Parcel parcel = withReceiver(fixture.authoritative, fixture.playerId);
        IllegalStateException databaseFailure = new IllegalStateException("database failed");
        fixture.stubCollect(parcel);
        when(fixture.parcelRepository.commitCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.failedFuture(databaseFailure));
        doThrow(new IllegalStateException("notice failed"))
            .when(fixture.noticeService).player(eq(fixture.playerId), any());

        try (MockedStatic<com.eternalcode.parcellockers.util.InventoryUtil> inventory =
                 mockStatic(com.eternalcode.parcellockers.util.InventoryUtil.class)) {
            inventory.when(() -> canHold(fixture.player, List.of())).thenReturn(true);
            CompletableFuture<Void> result = fixture.service.collect(fixture.player, parcel);
            fixture.runNextMainTask();

            CompletionException exception = assertThrows(CompletionException.class, result::join);
            ParcelOperationException operationException =
                assertInstanceOf(ParcelOperationException.class, exception.getCause());
            assertSame(databaseFailure, operationException.getCause());
        }
    }

    @Test
    void collectFutureCompletesOnlyAfterMainThreadItemsAndNotice() {
        CollectFixture fixture = new CollectFixture();
        Parcel parcel = withReceiver(fixture.authoritative, fixture.playerId);
        ItemStack item = mock(ItemStack.class);
        fixture.stubCollect(parcel, item);
        when(fixture.parcelRepository.commitCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        try (MockedStatic<com.eternalcode.parcellockers.util.InventoryUtil> inventory =
                 mockStatic(com.eternalcode.parcellockers.util.InventoryUtil.class);
             MockedStatic<ItemUtil> itemUtil = mockStatic(ItemUtil.class)) {
            inventory.when(() -> canHold(fixture.player, List.of(item))).thenReturn(true);

            CompletableFuture<Void> result = fixture.service.collect(fixture.player, parcel);

            assertFalse(result.isDone());
            fixture.runNextMainTask();
            assertFalse(result.isDone());
            fixture.runNextMainTask();
            itemUtil.verify(() -> ItemUtil.giveItem(fixture.player, item));
            verify(fixture.noticeService).player(
                eq(fixture.playerId), any());
            verify(fixture.collectedRepository, never()).save(any());
            assertTrue(result.isDone());
            result.join();
        }
    }

    @Test
    void collectSchedulerFailureRollsBackCommittedCollection() {
        CollectFixture fixture = new CollectFixture();
        Parcel parcel = withReceiver(fixture.authoritative, fixture.playerId);
        fixture.stubCollect(parcel);
        when(fixture.parcelRepository.commitCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(fixture.parcelRepository.rollbackCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        AtomicInteger scheduled = new AtomicInteger();
        doAnswer(invocation -> {
            if (scheduled.getAndIncrement() == 0) {
                fixture.mainTasks.add(invocation.getArgument(0));
                return null;
            }
            throw new IllegalStateException("scheduler rejected delivery");
        }).when(fixture.scheduler).run(any(Runnable.class));

        try (MockedStatic<com.eternalcode.parcellockers.util.InventoryUtil> inventory =
                 mockStatic(com.eternalcode.parcellockers.util.InventoryUtil.class)) {
            inventory.when(() -> canHold(fixture.player, List.of())).thenReturn(true);
            CompletableFuture<Void> result = fixture.service.collect(fixture.player, parcel);
            fixture.runNextMainTask();

            assertThrows(CompletionException.class, result::join);
            verify(fixture.parcelRepository)
                .rollbackCollection(eq(parcel.uuid()), eq(fixture.playerId), any());
        }
    }

    @Test
    void collectGiveFailureRestoresInventoryAndRollsBackCollection() {
        CollectFixture fixture = new CollectFixture();
        Parcel parcel = withReceiver(fixture.authoritative, fixture.playerId);
        ItemStack item = mock(ItemStack.class);
        ItemStack existing = mock(ItemStack.class);
        ItemStack existingSnapshot = mock(ItemStack.class);
        PlayerInventory playerInventory = mock(PlayerInventory.class);
        ItemStack[] snapshot = {existing};
        fixture.stubCollect(parcel, item);
        when(fixture.player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(snapshot);
        when(existing.clone()).thenReturn(existingSnapshot);
        when(fixture.parcelRepository.commitCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(fixture.parcelRepository.rollbackCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        try (MockedStatic<com.eternalcode.parcellockers.util.InventoryUtil> inventory =
                 mockStatic(com.eternalcode.parcellockers.util.InventoryUtil.class);
             MockedStatic<ItemUtil> itemUtil = mockStatic(ItemUtil.class)) {
            inventory.when(() -> canHold(fixture.player, List.of(item))).thenReturn(true);
            itemUtil.when(() -> ItemUtil.giveItem(fixture.player, item))
                .thenThrow(new IllegalStateException("give failed"));
            CompletableFuture<Void> result = fixture.service.collect(fixture.player, parcel);
            fixture.runNextMainTask();
            fixture.runNextMainTask();

            assertThrows(CompletionException.class, result::join);
            ArgumentCaptor<ItemStack[]> restored =
                ArgumentCaptor.forClass(ItemStack[].class);
            verify(playerInventory).setContents(restored.capture());
            assertSame(existingSnapshot, restored.getValue()[0]);
            verify(fixture.parcelRepository)
                .rollbackCollection(eq(parcel.uuid()), eq(fixture.playerId), any());
        }
    }

    @Test
    void collectDoesNotReopenParcelWhenInventoryRestoreFails() {
        CollectFixture fixture = new CollectFixture();
        Parcel parcel = withReceiver(fixture.authoritative, fixture.playerId);
        ItemStack item = mock(ItemStack.class);
        ItemStack existing = mock(ItemStack.class);
        PlayerInventory playerInventory = mock(PlayerInventory.class);
        fixture.stubCollect(parcel, item);
        when(fixture.player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(new ItemStack[]{existing});
        when(existing.clone()).thenReturn(mock(ItemStack.class));
        when(fixture.parcelRepository.commitCollection(
            eq(parcel.uuid()), eq(fixture.playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        doThrow(new IllegalStateException("restore failed"))
            .when(playerInventory).setContents(any(ItemStack[].class));

        try (MockedStatic<com.eternalcode.parcellockers.util.InventoryUtil> inventory =
                 mockStatic(com.eternalcode.parcellockers.util.InventoryUtil.class);
             MockedStatic<ItemUtil> itemUtil = mockStatic(ItemUtil.class)) {
            inventory.when(() -> canHold(fixture.player, List.of(item))).thenReturn(true);
            itemUtil.when(() -> ItemUtil.giveItem(fixture.player, item))
                .thenThrow(new IllegalStateException("give failed"));
            CompletableFuture<Void> result = fixture.service.collect(fixture.player, parcel);
            fixture.runNextMainTask();
            fixture.runNextMainTask();

            assertThrows(CompletionException.class, result::join);
            verify(fixture.parcelRepository, never())
                .rollbackCollection(any(), any(), any());
        }
    }

    @Test
    void duplicateParcelUuidFailsBeforeChargeAndDoesNotDeleteExistingParcel() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository = mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        config.settings.smallParcelFee = 10.0;
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(sender.hasPermission("parcellockers.fee.bypass")).thenReturn(false);
        when(parcelRepository.saveIfAbsent(parcel))
            .thenReturn(CompletableFuture.completedFuture(false));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService, parcelRepository, contentRepository, collectedRepository,
            scheduler, config, economy, server);

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.send(sender, parcel, List.of(mock(ItemStack.class))).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
        verify(contentRepository, never()).save(any());
        verify(parcelRepository, never()).delete(parcel.uuid());
        verify(contentRepository, never()).delete(parcel.uuid());
    }

    @Test
    void feeNoticeFailureAfterWithdrawalRefundsAndCleansReservedParcel() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository = mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        config.settings.smallParcelFee = 10.0;
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(sender.hasPermission("parcellockers.fee.bypass")).thenReturn(false);
        when(parcelRepository.saveIfAbsent(parcel))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(economy.withdrawPlayer(sender, 10.0))
            .thenReturn(successfulEconomyResponse(10.0));
        when(economy.depositPlayer(sender, 10.0))
            .thenReturn(successfulEconomyResponse(10.0));
        when(noticeService.create()).thenThrow(new IllegalStateException("notice failed"));
        when(parcelRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(contentRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService, parcelRepository, contentRepository, collectedRepository,
            scheduler, config, economy, server);

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.send(sender, parcel, List.of(mock(ItemStack.class))).join());

        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        verify(economy).depositPlayer(sender, 10.0);
        verify(parcelRepository).delete(parcel.uuid());
        verify(contentRepository).delete(parcel.uuid());
    }

    @Test
    void rollbackPermissionFailureDoesNotSkipParcelAndContentCleanup() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository = mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        IllegalStateException permissionFailure = new IllegalStateException("permission failed");
        when(sender.hasPermission("parcellockers.fee.bypass")).thenThrow(permissionFailure);
        when(parcelRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(contentRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService, parcelRepository, contentRepository, collectedRepository,
            scheduler, config, economy, server);

        CompletableFuture<Void> result =
            assertDoesNotThrow(() -> service.rollbackSend(sender, parcel));
        CompletionException exception = assertThrows(CompletionException.class, result::join);

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(permissionFailure, operationException.getCause());
        verify(parcelRepository).delete(parcel.uuid());
        verify(contentRepository).delete(parcel.uuid());
    }

    @Test
    void collectDefersSynchronousEventToMainSchedulerWhenCalledAsync() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository =
            mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Player player = mock(Player.class);
        Parcel parcel = new Parcel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "name",
            null,
            false,
            UUID.randomUUID(),
            ParcelSize.SMALL,
            UUID.randomUUID(),
            UUID.randomUUID(),
            ParcelStatus.DELIVERED
        );
        when(server.isPrimaryThread()).thenReturn(false);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(player.getUniqueId()).thenReturn(parcel.receiver());
        when(parcelRepository.findById(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(parcel)));
        when(contentRepository.find(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(java.util.Optional.empty()));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService,
            parcelRepository,
            contentRepository,
            collectedRepository,
            scheduler,
            config,
            economy,
            server
        );

        CompletableFuture<Void> result = service.collect(player, parcel);

        verify(pluginManager, never()).callEvent(any(ParcelCollectEvent.class));
        ArgumentCaptor<Runnable> eventTask = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).run(eventTask.capture());
        eventTask.getValue().run();
        verify(pluginManager).callEvent(any(ParcelCollectEvent.class));
        result.join();
    }

    @Test
    void contentPersistenceFailureAttemptsParcelAndContentCleanupAndPreservesTrigger() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository =
            mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        IllegalStateException contentSaveFailure =
            new IllegalStateException("content save failed");
        IllegalStateException parcelDeleteFailure =
            new IllegalStateException("parcel delete failed");
        IllegalStateException contentDeleteFailure =
            new IllegalStateException("content delete failed");
        when(sender.hasPermission("parcellockers.fee.bypass")).thenReturn(true);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(parcelRepository.saveIfAbsent(parcel)).thenReturn(CompletableFuture.completedFuture(true));
        when(contentRepository.save(any()))
            .thenReturn(CompletableFuture.failedFuture(contentSaveFailure));
        when(parcelRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.failedFuture(parcelDeleteFailure));
        when(contentRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.failedFuture(contentDeleteFailure));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService,
            parcelRepository,
            contentRepository,
            collectedRepository,
            scheduler,
            config,
            economy,
            server
        );

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.send(sender, parcel, List.of(mock(org.bukkit.inventory.ItemStack.class)))
                .join());

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(contentSaveFailure, operationException.getCause());
        assertEquals(
            List.of(parcelDeleteFailure, contentDeleteFailure),
            List.of(operationException.getSuppressed())
        );
        verify(parcelRepository).delete(parcel.uuid());
        verify(contentRepository).delete(parcel.uuid());
    }

    @Test
    void rollbackSendAttemptsFeeParcelAndContentCleanupAndAggregatesFailures() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository =
            mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        config.settings.smallParcelFee = 10.0;
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        IllegalStateException refundFailure = new IllegalStateException("refund failed");
        IllegalStateException parcelDeleteFailure =
            new IllegalStateException("parcel delete failed");
        IllegalStateException contentDeleteFailure =
            new IllegalStateException("content delete failed");
        when(sender.hasPermission("parcellockers.fee.bypass")).thenReturn(false);
        doThrow(refundFailure).when(economy).depositPlayer(sender, 10.0);
        when(parcelRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.failedFuture(parcelDeleteFailure));
        when(contentRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.failedFuture(contentDeleteFailure));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService,
            parcelRepository,
            contentRepository,
            collectedRepository,
            scheduler,
            config,
            economy,
            server
        );

        CompletableFuture<Void> result =
            assertDoesNotThrow(() -> service.rollbackSend(sender, parcel));
        CompletionException exception =
            assertThrows(CompletionException.class, result::join);

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertSame(refundFailure, operationException.getCause());
        assertEquals(
            List.of(parcelDeleteFailure, contentDeleteFailure),
            List.of(operationException.getSuppressed())
        );
        verify(economy).depositPlayer(sender, 10.0);
        verify(parcelRepository).delete(parcel.uuid());
        verify(contentRepository).delete(parcel.uuid());
    }

    @Test
    void rollbackSendTreatsFalseDeleteResultsAsCleanupFailures() {
        NoticeService noticeService = mock(NoticeService.class);
        ParcelRepository parcelRepository = mock(ParcelRepository.class);
        ParcelContentRepository contentRepository = mock(ParcelContentRepository.class);
        CollectedParcelRepository collectedRepository =
            mock(CollectedParcelRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        PluginConfig config = new PluginConfig();
        Economy economy = mock(Economy.class);
        Server server = mock(Server.class);
        Player sender = mock(Player.class);
        Parcel parcel = parcel();
        when(sender.hasPermission("parcellockers.fee.bypass")).thenReturn(true);
        when(parcelRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(contentRepository.delete(parcel.uuid()))
            .thenReturn(CompletableFuture.completedFuture(false));
        ParcelServiceImpl service = new ParcelServiceImpl(
            noticeService,
            parcelRepository,
            contentRepository,
            collectedRepository,
            scheduler,
            config,
            economy,
            server
        );

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.rollbackSend(sender, parcel).join());

        ParcelOperationException operationException =
            assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertInstanceOf(IllegalStateException.class, operationException.getCause());
        assertEquals(1, operationException.getSuppressed().length);
        assertInstanceOf(
            IllegalStateException.class, operationException.getSuppressed()[0]);
        verify(parcelRepository).delete(parcel.uuid());
        verify(contentRepository).delete(parcel.uuid());
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

    private static Parcel withReceiver(Parcel parcel, UUID receiver) {
        return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
            parcel.priority(), receiver, parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), ParcelStatus.DELIVERED);
    }

    private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
        return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
            parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), status);
    }

    private static EconomyResponse successfulEconomyResponse(double amount) {
        return new EconomyResponse(
            amount, 100.0, EconomyResponse.ResponseType.SUCCESS, null);
    }

    private static final class CollectFixture {

        private final NoticeService noticeService = mock(NoticeService.class);
        private final ParcelRepository parcelRepository = mock(ParcelRepository.class);
        private final ParcelContentRepository contentRepository =
            mock(ParcelContentRepository.class);
        private final CollectedParcelRepository collectedRepository =
            mock(CollectedParcelRepository.class);
        private final Scheduler scheduler = mock(Scheduler.class);
        private final PluginConfig config = new PluginConfig();
        private final Economy economy = mock(Economy.class);
        private final Server server = mock(Server.class);
        private final PluginManager pluginManager = mock(PluginManager.class);
        private final Player player = mock(Player.class);
        private final PlayerInventory playerInventory = mock(PlayerInventory.class);
        private final UUID playerId = UUID.randomUUID();
        private final Queue<Runnable> mainTasks = new ArrayDeque<>();
        private final Parcel authoritative = new Parcel(
            UUID.randomUUID(), UUID.randomUUID(), "name", null, false, UUID.randomUUID(),
            ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.DELIVERED);
        private final ParcelServiceImpl service;

        private CollectFixture() {
            when(this.player.getUniqueId()).thenReturn(this.playerId);
            when(this.player.getInventory()).thenReturn(this.playerInventory);
            when(this.playerInventory.getContents()).thenReturn(new ItemStack[0]);
            when(this.server.isPrimaryThread()).thenReturn(true);
            when(this.server.getPluginManager()).thenReturn(this.pluginManager);
            doAnswer(invocation -> {
                this.mainTasks.add(invocation.getArgument(0));
                return null;
            }).when(this.scheduler).run(any(Runnable.class));
            this.service = new ParcelServiceImpl(
                this.noticeService, this.parcelRepository, this.contentRepository,
                this.collectedRepository, this.scheduler, this.config, this.economy, this.server);
        }

        private void stubCollect(Parcel parcel, ItemStack... items) {
            when(this.parcelRepository.findById(parcel.uuid()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(parcel)));
            ParcelContent content = new ParcelContent(parcel.uuid(), List.of(items));
            when(this.contentRepository.find(parcel.uuid()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
        }

        private void runNextMainTask() {
            Runnable task = this.mainTasks.remove();
            task.run();
        }
    }
}
