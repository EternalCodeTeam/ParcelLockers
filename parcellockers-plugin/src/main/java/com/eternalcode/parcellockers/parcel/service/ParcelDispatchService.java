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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelDispatchService {

    private static final Logger LOGGER = Logger.getLogger(ParcelDispatchService.class.getName());

    private final LockerManager lockerManager;
    private final PluginParcelService parcelService;
    private final DeliveryManager deliveryManager;
    private final ItemStorageManager itemStorageManager;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final NoticeService noticeService;

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
        NoticeService noticeService
    ) {
        this.lockerManager = lockerManager;
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
        this.itemStorageManager = itemStorageManager;
        this.scheduler = scheduler;
        this.config = config;
        this.noticeService = noticeService;
    }

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
        return this.lockerManager.isLockerFull(parcel.destinationLocker())
            .thenCompose(isFull -> {
                if (isFull) {
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.lockerFull);
                    return CompletableFuture.completedFuture(false);
                }

                Duration delay = parcel.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;

                return this.parcelService.send(sender, parcel, items)
                    .thenCompose(success -> {
                        if (!Boolean.TRUE.equals(success)) {
                            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                            return CompletableFuture.completedFuture(false);
                        }

                        return this.itemStorageManager.delete(sender.getUniqueId())
                            // A failed delete must trigger the rollback, not skip straight to the outer
                            // exceptionally handler (which would leave the parcel sent and the fee charged).
                            .exceptionally(throwable -> false)
                            .thenCompose(deleted -> {
                                if (!Boolean.TRUE.equals(deleted)) {
                                    // The parcel and its content were already persisted and the fee charged,
                                    // but the sender's staged storage could not be cleared. Fully roll back
                                    // (parcel + content + fee) instead of leaving orphaned content behind.
                                    this.notifyCannotSend(sender);
                                    return this.rollback(sender, parcel);
                                }

                                return this.createDelivery(sender, parcel, items, delay);
                            });
                    });
            })
            .exceptionally(throwable -> {
                Throwable cause = unwrap(throwable);
                if (cause instanceof CompensationException compensationException) {
                    throw compensationException;
                }
                LOGGER.severe("Failed to dispatch parcel for player " + sender.getName() + ": " + throwable.getMessage());
                this.notifyCannotSend(sender);
                return false;
            });
    }

    private CompletableFuture<Boolean> createDelivery(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        Duration delay
    ) {
        CompletableFuture<?> deliveryCreated;
        try {
            deliveryCreated = this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));
        } catch (Throwable throwable) {
            deliveryCreated = CompletableFuture.failedFuture(throwable);
        }

        return deliveryCreated.handle((delivery, throwable) -> {
            if (throwable != null) {
                return this.restoreAndRollback(sender, parcel, items);
            }
            return this.scheduleDelivery(sender, parcel, items, delay);
        }).thenCompose(Function.identity());
    }

    private CompletableFuture<Boolean> scheduleDelivery(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        Duration delay
    ) {
        ParcelSendTask task = new ParcelSendTask(
            parcel,
            this.parcelService,
            this.deliveryManager,
            this.scheduler
        );

        try {
            this.scheduler.runLaterAsync(task, delay);
        } catch (Throwable throwable) {
            return this.deleteDeliveryRestoreAndRollback(sender, parcel, items);
        }

        try {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
        } catch (Throwable throwable) {
            LOGGER.warning("Parcel " + parcel.uuid()
                + " was committed, but its success notice failed: " + throwable.getMessage());
        }
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> deleteDeliveryRestoreAndRollback(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    ) {
        return this.compensationStep(
                () -> this.deliveryManager.delete(parcel.uuid()),
                "Failed to delete delivery " + parcel.uuid() + " during dispatch compensation"
            )
            .thenCompose(deleted -> Boolean.TRUE.equals(deleted)
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.failedFuture(new CompensationException(
                    "Delivery " + parcel.uuid() + " was not deleted during dispatch compensation"
                )))
            .thenCompose(ignored -> this.restoreStorage(sender, items))
            .thenCompose(ignored -> this.rollbackParcel(sender, parcel))
            .thenApply(ignored -> false);
    }

    private CompletableFuture<Boolean> restoreAndRollback(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    ) {
        return this.restoreStorage(sender, items)
            .thenCompose(ignored -> this.rollbackParcel(sender, parcel))
            .thenApply(ignored -> false);
    }

    private CompletableFuture<Boolean> rollback(Player sender, Parcel parcel) {
        return this.rollbackParcel(sender, parcel).thenApply(ignored -> false);
    }

    private CompletableFuture<?> restoreStorage(Player sender, List<ItemStack> items) {
        return this.compensationStep(
            () -> this.itemStorageManager.create(sender.getUniqueId(), items),
            "Failed to restore item storage for player " + sender.getUniqueId()
        );
    }

    private CompletableFuture<Void> rollbackParcel(Player sender, Parcel parcel) {
        return this.compensationStep(
            () -> this.parcelService.rollbackSend(sender, parcel),
            "Failed to roll back parcel " + parcel.uuid()
        );
    }

    private <T> CompletableFuture<T> compensationStep(
        Supplier<CompletableFuture<T>> action,
        String failureMessage
    ) {
        try {
            return action.get().exceptionallyCompose(throwable ->
                CompletableFuture.failedFuture(
                    new CompensationException(failureMessage, unwrap(throwable))));
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(
                new CompensationException(failureMessage, unwrap(throwable)));
        }
    }

    private void notifyCannotSend(Player sender) {
        try {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
        } catch (Throwable throwable) {
            LOGGER.warning("Failed to send dispatch failure notice to player "
                + sender.getName() + ": " + throwable.getMessage());
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private static final class CompensationException extends ParcelOperationException {

        private CompensationException(String message) {
            super(message);
        }

        private CompensationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
