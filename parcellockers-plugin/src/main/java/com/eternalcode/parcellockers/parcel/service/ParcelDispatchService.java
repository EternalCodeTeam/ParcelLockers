package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageReservation;
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
import java.util.logging.Level;
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
        ItemStorageReservation reservation = this.itemStorageManager
            .reserve(sender.getUniqueId())
            .orElse(null);
        if (reservation == null) {
            this.notifyCannotSend(sender);
            return CompletableFuture.completedFuture(false);
        }

        try {
            UUID lockerId = parcel.destinationLocker();

            CompletableFuture<Boolean> chained =
                this.lockerChains.compute(lockerId, (id, previous) -> {
                    CompletableFuture<?> predecessor = previous == null
                        ? CompletableFuture.completedFuture(null)
                        : previous.exceptionally(throwable -> null);
                    return predecessor.thenCompose(ignored ->
                        this.dispatchInternal(sender, parcel, items, reservation));
                });

            CompletableFuture<Boolean> draining = chained.whenComplete(
                (result, throwable) -> this.lockerChains.remove(lockerId, chained));
            return draining.whenComplete((result, throwable) -> reservation.close());
        } catch (Throwable throwable) {
            reservation.close();
            this.notifyCannotSend(sender);
            return CompletableFuture.failedFuture(this.operationFailure(
                "Failed to start dispatch for parcel " + parcel.uuid(),
                throwable
            ));
        }
    }

    private CompletableFuture<Boolean> dispatchInternal(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        ItemStorageReservation reservation
    ) {
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

                        return this.deleteStorage(reservation)
                            .handle((deleted, throwable) -> {
                                if (throwable != null) {
                                    return this.compensate(
                                        sender,
                                        parcel,
                                        "Failed to delete sender storage for parcel "
                                            + parcel.uuid(),
                                        unwrap(throwable),
                                        List.of(this.rollbackStep(sender, parcel))
                                    );
                                }
                                if (!Boolean.TRUE.equals(deleted)) {
                                    return this.compensate(
                                        sender,
                                        parcel,
                                        "Sender storage was not deleted for parcel "
                                            + parcel.uuid(),
                                        new IllegalStateException(
                                            "Sender storage delete returned false"),
                                        List.of(this.rollbackStep(sender, parcel))
                                    );
                                }
                                return this.createDelivery(
                                    sender, parcel, items, delay, reservation);
                            })
                            .thenCompose(Function.identity());
                    });
            })
            .handle((result, throwable) -> {
                if (throwable == null) {
                    return CompletableFuture.completedFuture(result);
                }
                ParcelOperationException failure = this.operationFailure(
                    "Failed to dispatch parcel " + parcel.uuid()
                        + " for sender " + sender.getUniqueId(),
                    throwable
                );
                LOGGER.log(
                    Level.SEVERE,
                    "Failed to dispatch parcel " + parcel.uuid()
                        + " for sender " + sender.getUniqueId(),
                    failure);
                this.notifyCannotSend(sender);
                return CompletableFuture.<Boolean>failedFuture(failure);
            })
            .thenCompose(Function.identity());
    }

    private CompletableFuture<Boolean> createDelivery(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        Duration delay,
        ItemStorageReservation reservation
    ) {
        CompletableFuture<?> deliveryCreated;
        try {
            deliveryCreated = this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));
        } catch (Throwable throwable) {
            deliveryCreated = CompletableFuture.failedFuture(throwable);
        }

        return deliveryCreated.handle((delivery, throwable) -> {
            if (throwable != null) {
                return this.compensate(
                    sender,
                    parcel,
                    "Failed to persist delivery for parcel " + parcel.uuid(),
                    unwrap(throwable),
                    List.of(
                        this.restoreStep(reservation, items),
                        this.rollbackStep(sender, parcel)
                    )
                );
            }
            return this.scheduleDelivery(sender, parcel, items, delay, reservation);
        }).thenCompose(Function.identity());
    }

    private CompletableFuture<Boolean> scheduleDelivery(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        Duration delay,
        ItemStorageReservation reservation
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
            return this.compensate(
                sender,
                parcel,
                "Failed to schedule delivery for parcel " + parcel.uuid(),
                throwable,
                List.of(
                    this.deliveryDeleteStep(parcel),
                    this.restoreStep(reservation, items),
                    this.rollbackStep(sender, parcel)
                )
            );
        }

        try {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
        } catch (Throwable throwable) {
            LOGGER.warning("Parcel " + parcel.uuid()
                + " was committed, but its success notice failed: " + throwable.getMessage());
        }
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> compensate(
        Player sender,
        Parcel parcel,
        String message,
        Throwable trigger,
        List<CleanupStep> steps
    ) {
        ParcelOperationException failure =
            new ParcelOperationException(message, unwrap(trigger));
        CompletableFuture<Void> cleanup = CompletableFuture.completedFuture(null);
        for (CleanupStep step : steps) {
            cleanup = cleanup.thenCompose(ignored ->
                this.attemptCleanup(step, failure));
        }
        return cleanup.thenCompose(ignored -> CompletableFuture.failedFuture(failure));
    }

    private CompletableFuture<Boolean> deleteStorage(
        ItemStorageReservation reservation
    ) {
        try {
            return reservation.delete();
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private CompletableFuture<Void> attemptCleanup(
        CleanupStep step,
        ParcelOperationException failure
    ) {
        CompletableFuture<?> action;
        try {
            action = step.action().get();
        } catch (Throwable throwable) {
            failure.addSuppressed(unwrap(throwable));
            return CompletableFuture.completedFuture(null);
        }
        if (action == null) {
            failure.addSuppressed(new IllegalStateException(
                step.description() + " returned a null future"));
            return CompletableFuture.completedFuture(null);
        }
        return action.handle((ignored, throwable) -> {
            if (throwable != null) {
                failure.addSuppressed(unwrap(throwable));
            }
            return null;
        });
    }

    private CleanupStep deliveryDeleteStep(Parcel parcel) {
        return new CleanupStep(
            "Delete delivery " + parcel.uuid(),
            () -> this.deliveryManager.delete(parcel.uuid()).thenCompose(deleted ->
                Boolean.TRUE.equals(deleted)
                    ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.failedFuture(new IllegalStateException(
                        "Delivery " + parcel.uuid()
                            + " was not deleted during dispatch compensation")))
        );
    }

    private CleanupStep restoreStep(
        ItemStorageReservation reservation,
        List<ItemStack> items
    ) {
        return new CleanupStep(
            "Restore sender storage " + reservation.owner(),
            () -> reservation.restore(items)
        );
    }

    private CleanupStep rollbackStep(Player sender, Parcel parcel) {
        return new CleanupStep(
            "Roll back parcel " + parcel.uuid(),
            () -> this.parcelService.rollbackSend(sender, parcel)
        );
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

    private ParcelOperationException operationFailure(String message, Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ParcelOperationException operationException) {
            return operationException;
        }
        return new ParcelOperationException(message, cause);
    }

    private record CleanupStep(
        String description,
        Supplier<CompletableFuture<?>> action
    ) {
    }
}
