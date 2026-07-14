package com.eternalcode.parcellockers.returns;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.notice.provider.NoticeProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.Delivery;
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
import com.eternalcode.parcellockers.returns.repository.ParcelReturnRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
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
    private final ReturnMismatchFormatter mismatchFormatter;
    private final ParcelReturnRepository parcelReturnRepository;
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
        ReturnMismatchFormatter mismatchFormatter,
        ParcelReturnRepository parcelReturnRepository,
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
        this.mismatchFormatter = mismatchFormatter;
        this.parcelReturnRepository = parcelReturnRepository;
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
        boolean feeBypassed = player.hasPermission(PARCEL_FEE_BYPASS_PERMISSION);

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
                    ParcelReturnValidationResult validation = this.validator.validate(deposited, optionalContent.get().items());
                    if (!validation.matches()) {
                        return this.abortMismatch(player, deposited, validation);
                    }

                    // The return ships to the original entry locker. It may have been deleted
                    // while the parcel was in the receiver's return window.
                    return this.lockerManager.get(current.entryLocker()).thenCompose(locker -> {
                        if (locker.isEmpty()) {
                            return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
                        }
                        return this.lockerManager.isLockerFull(current.entryLocker()).thenCompose(isFull -> {
                            if (Boolean.TRUE.equals(isFull)) {
                                return this.abort(player, deposited, messages -> messages.parcel.lockerFull);
                            }
                            return this.execute(player, current, deposited, feeBypassed);
                        });
                    });
                });
            });
        }).exceptionally(throwable -> {
            LOGGER.log(Level.SEVERE, "Failed to return parcel " + parcel.uuid() + " for " + player.getName(), throwable);
            this.giveBack(player, deposited);
            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
            return null;
        });
    }

    private CompletableFuture<Void> execute(
        Player player,
        Parcel current,
        List<ItemStack> deposited,
        boolean feeBypassed
    ) {
        double fee = feeBypassed ? 0 : this.returnFeeFor(current.size());
        if (fee <= 0) {
            return this.proceedWithReturn(player, current, deposited, 0);
        }

        // Vault economy providers expect withdrawPlayer to run on the primary thread; by this
        // point the call chain has already hopped onto a repository/DB executor thread, so the
        // withdrawal must be scheduled back onto the main thread rather than called in place.
        return this.scheduler.complete(() -> this.economy.withdrawPlayer(player, fee).transactionSuccess())
            .thenCompose(success -> {
                if (!Boolean.TRUE.equals(success)) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.insufficientFunds)
                        .player(player.getUniqueId())
                        .placeholder(PLACEHOLDER_AMOUNT, String.format("%.2f", fee))
                        .send();
                    this.giveBack(player, deposited);
                    return CompletableFuture.completedFuture(null);
                }
                // The fee-withdrawn notice is sent only after the atomic return commit succeeds:
                // if this attempt loses the race and the fee is refunded, the player must not
                // already have been told the fee was withdrawn.
                return this.proceedWithReturn(player, current, deposited, fee);
            });
    }

    private CompletableFuture<Void> proceedWithReturn(Player player, Parcel current, List<ItemStack> deposited, double refundableFee) {
        String returnedName = this.config.settings.parcelReturnNameFormat.replace("{NAME}", current.name());
        Parcel returned = new Parcel(current.uuid(), current.receiver(), returnedName,
            current.description(), current.priority(), current.sender(), current.size(),
            current.destinationLocker(), current.entryLocker(), ParcelStatus.SENT);

        List<ItemStack> depositedCopy = deposited.stream().map(ItemStack::clone).toList();
        Duration delay = returned.priority()
            ? this.config.settings.priorityParcelSendDuration
            : this.config.settings.parcelSendDuration;
        Delivery delivery = new Delivery(returned.uuid(), Instant.now().plus(delay));

        // Claim, content replacement, delivery persistence and collected-row cleanup are one
        // transaction. A losing concurrent attempt cannot overwrite the winner's content.
        return this.parcelReturnRepository.commit(
            returned,
            new ParcelContent(returned.uuid(), depositedCopy),
            delivery
        ).thenCompose(committed -> {
            if (!Boolean.TRUE.equals(committed)) {
                this.refund(player, refundableFee);
                return this.abort(player, deposited, messages -> messages.parcel.cannotReturn);
            }

            // Past the commit point the durable delivery row makes this shipment recoverable at
            // startup. Nothing below may route to refund/give-back recovery.
            try {
                this.parcelService.invalidate(returned.uuid());
                this.parcelContentManager.invalidate(returned.uuid());
                this.deliveryManager.invalidate(returned.uuid());

                if (refundableFee > 0) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.returnFeeWithdrawn)
                        .player(player.getUniqueId())
                        .placeholder(PLACEHOLDER_AMOUNT, String.format("%.2f", refundableFee))
                        .send();
                }

                this.scheduler.runLaterAsync(
                    new ParcelSendTask(returned, this.parcelService, this.deliveryManager, this.scheduler),
                    delay);

                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.returned);
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Post-commit step threw for returned parcel " + current.uuid()
                    + ": the return is already committed (parcel is SENT, content holds the deposited "
                    + "items) so no refund/give-back recovery was attempted", exception);
            }
            return CompletableFuture.completedFuture(null);
        })
            .exceptionally(throwable -> {
                this.refund(player, refundableFee);
                this.giveBack(player, deposited);
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotReturn);
                LOGGER.log(Level.SEVERE, "Failed to execute return of parcel " + current.uuid(), throwable);
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
            // Same primary-thread requirement as the withdrawal in execute(): this runs from
            // async continuations, so the deposit must be dispatched back onto the main thread.
            this.scheduler.run(() -> this.economy.depositPlayer(player, fee));
        }
    }

    private CompletableFuture<Void> abort(Player player, List<ItemStack> deposited, NoticeProvider<MessageConfig> notice) {
        this.giveBack(player, deposited);
        this.noticeService.player(player.getUniqueId(), notice);
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> abortMismatch(
        Player player,
        List<ItemStack> deposited,
        ParcelReturnValidationResult validation
    ) {
        this.giveBack(player, deposited);
        this.noticeService.create()
            .notice(this.mismatchFormatter.notice(validation))
            .player(player.getUniqueId())
            .placeholder("{MISMATCHES}", this.mismatchFormatter.format(validation))
            .send();
        return CompletableFuture.completedFuture(null);
    }

    private void giveBack(Player player, List<ItemStack> items) {
        this.scheduler.run(() -> items.forEach(item -> ItemUtil.giveItem(player, item)));
    }
}
