package com.eternalcode.parcellockers.returns;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.notice.provider.NoticeProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelReturnEvent;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Orchestrates returning a collected parcel: validates the deposited items against the stored
 * content snapshot, charges the return fee, flips the parcel into a reverse SENT shipment and
 * schedules the normal delivery task. Every abort path hands the deposited items back.
 */
public class ParcelReturnService {

    private static final Logger LOGGER = Logger.getLogger(ParcelReturnService.class.getName());
    private static final String PARCEL_FEE_BYPASS_PERMISSION = "parcellockers.fee.bypass";
    private static final String PLACEHOLDER_AMOUNT = "{AMOUNT}";

    private final ParcelService parcelService;
    private final ParcelContentManager parcelContentManager;
    private final CollectedParcelRepository collectedParcelRepository;
    private final DeliveryManager deliveryManager;
    private final LockerManager lockerManager;
    private final ParcelReturnValidator validator;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final NoticeService noticeService;
    private final Economy economy;
    private final Server server;

    public ParcelReturnService(
        ParcelService parcelService,
        ParcelContentManager parcelContentManager,
        CollectedParcelRepository collectedParcelRepository,
        DeliveryManager deliveryManager,
        LockerManager lockerManager,
        ParcelReturnValidator validator,
        Scheduler scheduler,
        PluginConfig config,
        NoticeService noticeService,
        Economy economy,
        Server server
    ) {
        this.parcelService = parcelService;
        this.parcelContentManager = parcelContentManager;
        this.collectedParcelRepository = collectedParcelRepository;
        this.deliveryManager = deliveryManager;
        this.lockerManager = lockerManager;
        this.validator = validator;
        this.scheduler = scheduler;
        this.config = config;
        this.noticeService = noticeService;
        this.economy = economy;
        this.server = server;
    }

    public static boolean isWithinReturnWindow(CollectedParcel collected, Duration window, Instant now) {
        return collected.collectedAt().plus(window).isAfter(now);
    }

    public CompletableFuture<Optional<CollectedParcel>> getCollectedInfo(UUID parcelId) {
        Objects.requireNonNull(parcelId, "Parcel UUID cannot be null");
        return this.collectedParcelRepository.find(parcelId);
    }

    public CompletableFuture<Void> returnParcel(Player player, Parcel parcel, List<ItemStack> deposited) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        Objects.requireNonNull(deposited, "Deposited items cannot be null");

        ParcelReturnEvent event = new ParcelReturnEvent(parcel);
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
        }

        return this.parcelService.get(parcel.uuid()).thenCompose(optionalParcel -> {
            if (optionalParcel.isEmpty()
                || optionalParcel.get().status() != ParcelStatus.COLLECTED
                || !player.getUniqueId().equals(optionalParcel.get().receiver())) {
                return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
            }
            Parcel current = optionalParcel.get();

            return this.collectedParcelRepository.find(current.uuid()).thenCompose(optionalCollected -> {
                if (optionalCollected.isEmpty()
                    || !isWithinReturnWindow(optionalCollected.get(), this.config.settings.parcelReturnWindow, Instant.now())) {
                    return this.abort(player, deposited, messages -> messages.parcel.returnWindowExpired);
                }

                return this.parcelContentManager.get(current.uuid()).thenCompose(optionalContent -> {
                    if (optionalContent.isEmpty()) {
                        return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
                    }
                    if (!this.validator.matches(deposited, optionalContent.get().items())) {
                        return this.abort(player, deposited, messages -> messages.parcel.returnItemsMismatch);
                    }

                    // The return ships to the original entry locker.
                    return this.lockerManager.isLockerFull(current.entryLocker()).thenCompose(isFull -> {
                        if (Boolean.TRUE.equals(isFull)) {
                            return this.abort(player, deposited, messages -> messages.parcel.lockerFull);
                        }
                        return this.execute(player, current, deposited);
                    });
                });
            });
        }).exceptionally(throwable -> {
            LOGGER.severe("Failed to return parcel " + parcel.uuid() + " for " + player.getName() + ": " + throwable.getMessage());
            this.giveBack(player, deposited);
            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
            return null;
        });
    }

    private CompletableFuture<Void> execute(Player player, Parcel current, List<ItemStack> deposited) {
        double chargedFee = 0;
        if (!player.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            double fee = this.returnFeeFor(current.size());
            if (fee > 0) {
                boolean success = this.economy.withdrawPlayer(player, fee).transactionSuccess();
                String formattedFee = String.format("%.2f", fee);
                if (!success) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.insufficientFunds)
                        .player(player.getUniqueId())
                        .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                        .send();
                    this.giveBack(player, deposited);
                    return CompletableFuture.completedFuture(null);
                }
                chargedFee = fee;
                this.noticeService.create()
                    .notice(messages -> messages.parcel.returnFeeWithdrawn)
                    .player(player.getUniqueId())
                    .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                    .send();
            }
        }
        double refundableFee = chargedFee;

        Parcel returned = new Parcel(current.uuid(), current.receiver(), current.name(),
            current.description(), current.priority(), current.sender(), current.size(),
            current.destinationLocker(), current.entryLocker(), ParcelStatus.SENT);

        List<ItemStack> depositedCopy = deposited.stream().map(ItemStack::clone).toList();

        // Content is overwritten with the actually-deposited items first (they may legitimately
        // differ from the snapshot when check flags are relaxed); only then the status flip makes
        // the parcel a live shipment. markReturned failing means a concurrent return/purge won.
        return this.parcelContentManager.update(current.uuid(), depositedCopy)
            .thenCompose(updated -> this.parcelService.markReturned(returned))
            .thenCompose(marked -> {
                if (!Boolean.TRUE.equals(marked)) {
                    this.refund(player, refundableFee);
                    return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
                }

                // Past the commit point: the parcel is now a live SENT shipment and its content IS the
                // deposited items. Nothing below may route to the refund/give-back recovery — undoing
                // here would duplicate the items.

                // Best-effort cleanup: a leftover row is ignored by the purge task because the
                // parcel is no longer COLLECTED.
                this.collectedParcelRepository.delete(current.uuid()).exceptionally(throwable -> {
                    LOGGER.warning("Failed to delete collected_parcels row for returned parcel "
                        + current.uuid() + ": " + throwable.getMessage());
                    return false;
                });

                Duration delay = returned.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;

                // Schedule the send task before persisting the delivery: ParcelSendTask.decide treats
                // a missing delivery row as "deliver when due", so the task is safe even if the
                // delivery upsert below fails.
                this.scheduler.runLaterAsync(
                    new ParcelSendTask(returned, this.parcelService, this.deliveryManager, this.scheduler),
                    delay);

                // Use update (upsert), not create: a stale delivery entry can still be cached for
                // this UUID from the parcel's original trip (its cleanup delete is best-effort and
                // re-cached on restart via cacheAll), and create() throws IllegalStateException on an
                // existing cache entry. Throwing here must not happen post-commit.
                this.deliveryManager.update(returned.uuid(), Instant.now().plus(delay))
                    .exceptionally(throwable -> {
                        LOGGER.severe("Failed to persist delivery for returned parcel " + returned.uuid()
                            + " (send task scheduled in-memory; a restart before it fires may strand the parcel): "
                            + throwable.getMessage());
                        return null;
                    });

                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.returned);
                return CompletableFuture.<Void>completedFuture(null);
            })
            .exceptionally(throwable -> {
                this.refund(player, refundableFee);
                this.giveBack(player, deposited);
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
                LOGGER.severe("Failed to execute return of parcel " + current.uuid() + ": " + throwable.getMessage());
                return null;
            });
    }

    private double returnFeeFor(ParcelSize size) {
        return switch (size) {
            case SMALL -> this.config.settings.smallParcelReturnFee;
            case MEDIUM -> this.config.settings.mediumParcelReturnFee;
            case LARGE -> this.config.settings.largeParcelReturnFee;
        };
    }

    private void refund(Player player, double fee) {
        if (fee > 0) {
            this.economy.depositPlayer(player, fee);
        }
    }

    private CompletableFuture<Void> abort(Player player, List<ItemStack> deposited, NoticeProvider<MessageConfig> notice) {
        this.giveBack(player, deposited);
        this.noticeService.player(player.getUniqueId(), notice);
        return CompletableFuture.completedFuture(null);
    }

    private void giveBack(Player player, List<ItemStack> items) {
        this.scheduler.run(() -> items.forEach(item -> ItemUtil.giveItem(player, item)));
    }
}
