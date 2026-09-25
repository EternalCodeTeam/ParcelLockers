package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelDispatchService {

    private final LockerManager lockerManager;
    private final PluginParcelService parcelService;
    private final DeliveryManager deliveryManager;
    private final ItemStorageManager itemStorageManager;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final NoticeService noticeService;
    private final Logger logger;

    // Serializes dispatches per destination locker so that two concurrent sends cannot both pass
    // the fullness check before either parcel is persisted (a TOCTOU that could exceed the cap).
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> lockerChains = new ConcurrentHashMap<>();

    public ParcelDispatchService(
        LockerManager lockerManager,
        PluginParcelService parcelService,
        DeliveryManager deliveryManager,
        ItemStorageManager itemStorageManager,
        Scheduler scheduler,
        PluginConfig config,
        NoticeService noticeService,
        Logger logger
    ) {
        this.lockerManager = lockerManager;
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
        this.itemStorageManager = itemStorageManager;
        this.scheduler = scheduler;
        this.config = config;
        this.noticeService = noticeService;
        this.logger = logger;
    }

    /**
     * Sends a parcel: checks the destination capacity, persists the parcel (charging the fee),
     * clears the sender's staged item storage and schedules the delivery.
     *
     * @return a future completed with {@code true} when the parcel was sent, {@code false} when it
     *     was refused (missing or full locker, cancelled event, insufficient funds), or exceptionally
     *     when a persistence step failed; a failure after the parcel was saved is rolled back, unless
     *     the sender's items could not be restored, in which case the parcel is kept so they are not lost
     */
    public CompletableFuture<Boolean> dispatch(Player sender, Parcel parcel, List<ItemStack> items) {
        UUID lockerId = parcel.destinationLocker();

        CompletableFuture<Boolean> chained = this.lockerChains.compute(lockerId, (id, previous) -> {
            CompletableFuture<?> predecessor = previous == null
                ? CompletableFuture.completedFuture(null)
                : previous.exceptionally(throwable -> null);
            return predecessor.thenCompose(ignored -> this.dispatchInternal(sender, parcel, items));
        });

        // Drop the chain entry once it drains so the map does not grow unbounded.
        chained.whenComplete((result, throwable) -> this.lockerChains.remove(lockerId, chained));
        return chained;
    }

    private CompletableFuture<Boolean> dispatchInternal(Player sender, Parcel parcel, List<ItemStack> items) {
        return this.lockerManager.get(parcel.destinationLocker())
            .thenCompose(destination -> {
                // The locker may have been removed since it was picked, or never existed for API callers.
                if (destination.isEmpty()) {
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.destinationNotFound);
                    return CompletableFuture.completedFuture(false);
                }
                return this.sendIfLockerHasSpace(sender, parcel, items);
            })
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    this.logger.log(Level.SEVERE, "Failed to dispatch parcel " + parcel.uuid()
                        + " for player " + sender.getName(), throwable);
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                }
            });
    }

    private CompletableFuture<Boolean> sendIfLockerHasSpace(Player sender, Parcel parcel, List<ItemStack> items) {
        return this.lockerManager.isLockerFull(parcel.destinationLocker()).thenCompose(isFull -> {
            if (isFull) {
                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.lockerFull);
                return CompletableFuture.completedFuture(false);
            }

            return this.parcelService.send(sender, parcel, items).thenCompose(success -> {
                if (!success) {
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                    return CompletableFuture.completedFuture(false);
                }
                return this.clearStorageAndScheduleDelivery(sender, parcel, items);
            });
        });
    }

    private CompletableFuture<Boolean> clearStorageAndScheduleDelivery(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    ) {
        UUID senderId = sender.getUniqueId();
        return this.itemStorageManager.delete(senderId)
            // A failed delete must trigger the rollback too, otherwise the parcel would stay sent
            // (and the fee charged) while the sender still holds the staged items.
            .exceptionally(throwable -> false)
            .thenCompose(deleted -> {
                if (!deleted) {
                    return this.rollback(sender, parcel,
                        new ParcelOperationException("Failed to clear item storage of " + senderId));
                }

                Duration delay = parcel.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;

                return this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay))
                    .thenApply(delivery -> {
                        this.scheduler.runLaterAsync(
                            new ParcelSendTask(parcel, this.parcelService, this.deliveryManager, this.scheduler),
                            delay);
                        // Only confirm success once every step has succeeded, to avoid a "sent" notice
                        // immediately followed by "cannot send" on a rollback.
                        this.noticeService.player(senderId, messages -> messages.parcel.sent);
                        return true;
                    })
                    .exceptionallyCompose(deliveryError -> this.restoreStorageThenRollback(sender, parcel, items, deliveryError));
            });
    }

    private CompletableFuture<Boolean> restoreStorageThenRollback(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        Throwable deliveryError
    ) {
        return this.itemStorageManager.create(sender.getUniqueId(), items)
            .handle((restored, restoreError) -> restoreError)
            .thenCompose(restoreError -> {
                if (restoreError == null) {
                    return this.rollback(sender, parcel,
                        new ParcelOperationException("Failed to create delivery for " + parcel.uuid(), deliveryError));
                }

                // The staged items are gone, so the parcel content is now their only copy - keep the parcel
                // instead of rolling it back, so an admin can still deliver or recover it.
                ParcelOperationException cause = new ParcelOperationException("Failed to create delivery for "
                    + parcel.uuid() + " and to restore the sender's items; the parcel and its content were kept"
                    + " for manual recovery", deliveryError);
                cause.addSuppressed(restoreError);
                return CompletableFuture.failedFuture(cause);
            });
    }

    private CompletableFuture<Boolean> rollback(Player sender, Parcel parcel, ParcelOperationException cause) {
        return this.parcelService.rollbackSend(sender, parcel)
            .handle((ignored, rollbackError) -> {
                if (rollbackError != null) {
                    cause.addSuppressed(rollbackError);
                }
                throw cause;
            });
    }
}
