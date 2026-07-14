package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.notice.NoticeBroadcast;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.returns.repository.ParcelReturnRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ParcelReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");
    private static final Duration WINDOW = Duration.ofDays(7);

    @Test
    void withinWindowJustAfterCollection() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofHours(1)));
        assertTrue(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void outsideWindowAfterExpiry() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofDays(8)));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void exactExpiryInstantIsOutsideWindow() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(WINDOW));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void waitsForDurableCommitBeforeSchedulingOrReportingSuccess() {
        Fixture fixture = new Fixture();
        fixture.validReturn();
        CompletableFuture<Boolean> commit = new CompletableFuture<>();
        when(fixture.returnRepository.commit(any(), any(), any())).thenReturn(commit);

        CompletableFuture<Void> result = fixture.service.returnParcel(
            fixture.player, fixture.parcel, fixture.deposited);

        assertFalse(result.isDone());
        verify(fixture.scheduler, never()).runLaterAsync(any(Runnable.class), any(Duration.class));
        verify(fixture.noticeService, never()).player(eq(fixture.playerId), any());

        commit.complete(true);
        result.join();

        verify(fixture.scheduler).runLaterAsync(any(Runnable.class), any(Duration.class));
        verify(fixture.noticeService).player(eq(fixture.playerId), any());
    }

    @Test
    void rejectsReturnWhenOriginalEntryLockerNoLongerExists() {
        Fixture fixture = new Fixture();
        fixture.validReturn();
        when(fixture.lockerManager.get(fixture.parcel.entryLocker()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        fixture.service.returnParcel(fixture.player, fixture.parcel, fixture.deposited).join();

        verify(fixture.lockerManager, never()).isLockerFull(any());
        verify(fixture.returnRepository, never()).commit(any(), any(), any());
        verify(fixture.economy, never()).withdrawPlayer(any(Player.class), anyDouble());
    }

    @Test
    void capturesFeeBypassPermissionBeforeRepositoryContinuation() {
        Fixture fixture = new Fixture();
        CompletableFuture<Optional<Parcel>> pendingParcel = new CompletableFuture<>();
        when(fixture.parcelService.get(fixture.parcel.uuid())).thenReturn(pendingParcel);

        fixture.service.returnParcel(fixture.player, fixture.parcel, fixture.deposited);

        verify(fixture.player).hasPermission("parcellockers.fee.bypass");
    }

    @Test
    void appliesDefaultPrefixToReturnedParcelName() {
        Fixture fixture = new Fixture();
        assertEquals("[Refund] parcel", fixture.returnedParcelName());
    }

    @Test
    void appliesCustomReturnedParcelNameFormat() {
        Fixture fixture = new Fixture();
        fixture.config.settings.parcelReturnNameFormat = "Returned: {NAME} / {NAME}";
        assertEquals("Returned: parcel / parcel", fixture.returnedParcelName());
    }

    @Test
    void usesLiteralReturnedParcelNameWhenFormatHasNoPlaceholder() {
        Fixture fixture = new Fixture();
        fixture.config.settings.parcelReturnNameFormat = "Returned parcel";
        assertEquals("Returned parcel", fixture.returnedParcelName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void reportsAllMismatchReasonsAndStopsReturnProcessing() {
        Fixture fixture = new Fixture();
        fixture.validReturn();
        ParcelReturnValidationResult mismatch = new ParcelReturnValidationResult(List.of(
            ReturnItemMismatch.insufficient(Material.DIAMOND, 5, 4),
            ReturnItemMismatch.unexpected(Material.DIRT, 2)
        ));
        when(fixture.validator.validate(fixture.deposited, fixture.deposited)).thenReturn(mismatch);
        when(fixture.mismatchFormatter.format(mismatch)).thenReturn("first<newline>second");
        NoticeBroadcast broadcast = mock(NoticeBroadcast.class, RETURNS_SELF);
        when(fixture.noticeService.create()).thenReturn(broadcast);

        fixture.service.returnParcel(fixture.player, fixture.parcel, fixture.deposited).join();

        verify(broadcast).placeholder("{MISMATCHES}", "first<newline>second");
        verify(broadcast).send();
        verify(fixture.scheduler).run(any(Runnable.class));
        verify(fixture.returnRepository, never()).commit(any(), any(), any());
        verify(fixture.economy, never()).withdrawPlayer(any(Player.class), anyDouble());
        verify(fixture.scheduler, never()).runLaterAsync(any(Runnable.class), any(Duration.class));
    }

    private static final class Fixture {

        private final UUID playerId = UUID.randomUUID();
        private final Player player = mock(Player.class);
        private final ParcelService parcelService = mock(ParcelService.class);
        private final ParcelContentManager contentManager = mock(ParcelContentManager.class);
        private final CollectedParcelRepository collectedRepository = mock(CollectedParcelRepository.class);
        private final DeliveryManager deliveryManager = mock(DeliveryManager.class);
        private final LockerManager lockerManager = mock(LockerManager.class);
        private final ParcelReturnValidator validator = mock(ParcelReturnValidator.class);
        private final ReturnMismatchFormatter mismatchFormatter = mock(ReturnMismatchFormatter.class);
        private final ParcelReturnRepository returnRepository = mock(ParcelReturnRepository.class);
        private final Scheduler scheduler = mock(Scheduler.class);
        private final NoticeService noticeService = mock(NoticeService.class);
        private final Economy economy = mock(Economy.class);
        private final Server server = mock(Server.class);
        private final PluginManager pluginManager = mock(PluginManager.class);
        private final PluginConfig config = new PluginConfig();
        private final ItemStack item = mock(ItemStack.class);
        private final List<ItemStack> deposited = List.of(this.item);
        private final Parcel parcel = new Parcel(
            UUID.randomUUID(), UUID.randomUUID(), "parcel", "description", false,
            this.playerId, ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED
        );
        private final ParcelReturnService service;

        private Fixture() {
            this.config.settings.smallParcelReturnFee = 0;
            when(this.player.getUniqueId()).thenReturn(this.playerId);
            when(this.player.getName()).thenReturn("Player");
            when(this.item.clone()).thenReturn(this.item);
            when(this.server.getPluginManager()).thenReturn(this.pluginManager);
            this.service = new ParcelReturnService(
                this.parcelService,
                this.contentManager,
                this.collectedRepository,
                this.deliveryManager,
                this.lockerManager,
                this.validator,
                this.mismatchFormatter,
                this.returnRepository,
                this.scheduler,
                this.config,
                this.noticeService,
                this.economy,
                this.server
            );
        }

        private void validReturn() {
            ParcelContent content = new ParcelContent(this.parcel.uuid(), this.deposited);
            when(this.parcelService.get(this.parcel.uuid()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(this.parcel)));
            when(this.collectedRepository.find(this.parcel.uuid()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(
                    new CollectedParcel(this.parcel.uuid(), Instant.now()))));
            when(this.contentManager.get(this.parcel.uuid()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(content)));
            when(this.validator.validate(this.deposited, this.deposited))
                .thenReturn(new ParcelReturnValidationResult(List.of()));
            when(this.lockerManager.get(this.parcel.entryLocker()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(
                    new Locker(this.parcel.entryLocker(), "Entry", null))));
            when(this.lockerManager.isLockerFull(this.parcel.entryLocker()))
                .thenReturn(CompletableFuture.completedFuture(false));
        }

        private String returnedParcelName() {
            this.validReturn();
            when(this.returnRepository.commit(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

            this.service.returnParcel(this.player, this.parcel, this.deposited).join();

            ArgumentCaptor<Parcel> returnedParcel = ArgumentCaptor.forClass(Parcel.class);
            verify(this.returnRepository).commit(returnedParcel.capture(), any(), any());
            return returnedParcel.getValue().name();
        }
    }
}
