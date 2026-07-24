package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelCollectEvent;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ParcelServiceImplTest {

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
        when(parcelRepository.save(parcel)).thenReturn(CompletableFuture.completedFuture(null));
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
}
