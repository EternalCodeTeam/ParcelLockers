package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class AdminParcelService {

    private final PluginParcelService parcelService;
    private final ParcelContentManager parcelContentManager;
    private final DeliveryManager deliveryManager;
    private final LockerManager lockerManager;
    private final PluginConfig config;
    private final Scheduler scheduler;

    public AdminParcelService(PluginParcelService parcelService, ParcelContentManager parcelContentManager,
            DeliveryManager deliveryManager, LockerManager lockerManager, PluginConfig config, Scheduler scheduler) {
        this.parcelService = parcelService;
        this.parcelContentManager = parcelContentManager;
        this.deliveryManager = deliveryManager;
        this.lockerManager = lockerManager;
        this.config = config;
        this.scheduler = scheduler;
    }

    public static int capacity(ParcelSize size) {
        return switch (size) {
            case SMALL -> 9;
            case MEDIUM -> 18;
            case LARGE -> 27;
        };
    }

    /** Pure delta-shift helper, clamped to never be before {@code now}. */
    public static Instant shiftedDeliveryTimestamp(Instant oldTimestamp, boolean oldPriority, boolean newPriority,
            Duration normalDuration, Duration priorityDuration, Instant now) {
        Duration oldDuration = oldPriority ? priorityDuration : normalDuration;
        Duration newDuration = newPriority ? priorityDuration : normalDuration;
        Instant shifted = oldTimestamp.plus(newDuration).minus(oldDuration);
        return shifted.isBefore(now) ? now : shifted;
    }

    public CompletableFuture<EditResult> changeName(Parcel parcel, String name) {
        return this.editAuthoritative(parcel, current -> withName(current, name));
    }

    public CompletableFuture<EditResult> changeDescription(Parcel parcel, String description) {
        return this.editAuthoritative(
            parcel, current -> withDescription(current, description));
    }

    /**
     * A COLLECTED parcel's content row is the collection snapshot, not a live shipment; flipping
     * its status (e.g. back to SENT) would arm a delivery and hand the receiver the same items
     * a second time. Refuse the change outright — a return is the only supported transition out
     * of COLLECTED.
     */
    public CompletableFuture<EditResult> changeStatus(Parcel parcel, ParcelStatus status) {
        return this.parcelService.serializeParcelOperation(
            parcel.uuid(), () -> this.changeStatusWithinParcelOperation(parcel, status));
    }

    private CompletableFuture<EditResult> changeStatusWithinParcelOperation(
        Parcel parcel,
        ParcelStatus status
    ) {
        return this.parcelService.getAuthoritativeWithinParcelOperation(parcel.uuid())
            .thenCompose(optional -> {
                if (optional.isEmpty()
                    || optional.get().status() == ParcelStatus.COLLECTED) {
                    return parcelUnavailable();
                }
                Parcel current = optional.get();
                if (status == current.status()) {
                    return CompletableFuture.completedFuture(EditResult.ok());
                }
                return this.deliveryManager.get(current.uuid())
                    .thenCompose(priorDelivery ->
                        this.applyStatusChange(current, status, priorDelivery));
            });
    }

    private CompletableFuture<EditResult> applyStatusChange(
        Parcel current,
        ParcelStatus status,
        Optional<Delivery> priorDelivery
    ) {
        Parcel updated = withStatus(current, status);
        return this.parcelService.updateIfStatusWithinParcelOperation(
            updated, current.status()).thenCompose(applied -> {
            if (!Boolean.TRUE.equals(applied)) {
                return parcelUnavailable();
            }
            CompletableFuture<EditResult> reconciliation = invoke(
                () -> status == ParcelStatus.SENT
                    ? this.armDelivery(updated, priorDelivery)
                    : this.disarmDelivery(updated, priorDelivery));
            return reconciliation.handle((result, throwable) -> {
                if (throwable == null) {
                    return CompletableFuture.completedFuture(result);
                }
                return this.compensateAndFail(
                    current, updated, priorDelivery, throwable);
            }).thenCompose(future -> future);
        });
    }

    private CompletableFuture<EditResult> armDelivery(
        Parcel parcel,
        Optional<Delivery> priorDelivery
    ) {
        if (priorDelivery.isPresent()) {
            this.scheduleSend(
                parcel,
                Duration.between(
                    Instant.now(), priorDelivery.get().deliveryTimestamp()));
            return CompletableFuture.completedFuture(EditResult.ok());
        }
        Duration delay = this.deliveryDelay(parcel.priority());
        return this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay))
            .thenApply(delivery -> {
                this.scheduleSend(parcel, delay);
                return EditResult.ok();
            });
    }

    private CompletableFuture<EditResult> disarmDelivery(
        Parcel parcel,
        Optional<Delivery> priorDelivery
    ) {
        if (priorDelivery.isEmpty()) {
            return CompletableFuture.completedFuture(EditResult.ok());
        }
        return this.deliveryManager.delete(parcel.uuid())
            .thenCompose(deleted -> Boolean.TRUE.equals(deleted)
                ? CompletableFuture.completedFuture(EditResult.ok())
                : CompletableFuture.failedFuture(
                    new IllegalStateException("Delivery delete returned false")));
    }

    private void scheduleSend(Parcel parcel, Duration delay) {
        this.scheduler.runLaterAsync(
            new ParcelSendTask(parcel, this.parcelService, this.deliveryManager, this.scheduler),
            delay.isNegative() ? Duration.ZERO : delay);
    }

    public CompletableFuture<EditResult> changeReceiver(Parcel parcel, UUID receiver) {
        return this.editAuthoritative(
            parcel, current -> withReceiver(current, receiver));
    }

    public CompletableFuture<EditResult> changeSize(Parcel parcel, ParcelSize newSize) {
        return this.parcelContentManager.get(parcel.uuid()).thenCompose(optional -> {
            int itemCount = optional.map(content -> content.items().size()).orElse(0);
            if (itemCount > capacity(newSize)) {
                return CompletableFuture.completedFuture(EditResult.of(EditResult.Status.SIZE_TOO_SMALL));
            }
            return this.editAuthoritative(
                parcel, current -> withSize(current, newSize));
        });
    }

    public CompletableFuture<EditResult> changeDestination(Parcel parcel, UUID destinationLocker) {
        return this.lockerManager.isLockerFull(destinationLocker).thenCompose(full -> {
            if (Boolean.TRUE.equals(full)) {
                return CompletableFuture.completedFuture(EditResult.of(EditResult.Status.DESTINATION_FULL));
            }
            return this.editAuthoritative(
                parcel, current -> withDestination(current, destinationLocker));
        });
    }

    public CompletableFuture<EditResult> changePriority(Parcel parcel, boolean newPriority) {
        return this.parcelService.serializeParcelOperation(
            parcel.uuid(), () -> this.changePriorityWithinParcelOperation(parcel, newPriority));
    }

    private CompletableFuture<EditResult> changePriorityWithinParcelOperation(
        Parcel parcel,
        boolean newPriority
    ) {
        return this.parcelService.getAuthoritativeWithinParcelOperation(parcel.uuid())
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return parcelUnavailable();
                }
                Parcel current = optional.get();
                if (newPriority == current.priority()) {
                    return CompletableFuture.completedFuture(EditResult.ok());
                }
                return this.deliveryManager.get(current.uuid())
                    .thenCompose(priorDelivery ->
                        this.applyPriorityChange(current, newPriority, priorDelivery));
            });
    }

    private CompletableFuture<EditResult> applyPriorityChange(
        Parcel current,
        boolean newPriority,
        Optional<Delivery> priorDelivery
    ) {
        Parcel updated = withPriority(current, newPriority);
        return this.parcelService.updateIfStatusWithinParcelOperation(
            updated, current.status()).thenCompose(applied -> {
            if (!Boolean.TRUE.equals(applied)) {
                return parcelUnavailable();
            }
            if (current.status() != ParcelStatus.SENT || priorDelivery.isEmpty()) {
                return CompletableFuture.completedFuture(EditResult.ok());
            }

            Instant now = Instant.now();
            Instant shifted = shiftedDeliveryTimestamp(
                priorDelivery.get().deliveryTimestamp(),
                current.priority(), newPriority,
                this.config.settings.parcelSendDuration,
                this.config.settings.priorityParcelSendDuration,
                now);
            CompletableFuture<EditResult> reconciliation = invoke(
                () -> this.deliveryManager.update(current.uuid(), shifted)
                    .thenApply(ignoredDelivery -> {
                        this.scheduleSend(updated, Duration.between(now, shifted));
                        return EditResult.ok();
                    }));
            return reconciliation.handle((result, throwable) -> {
                if (throwable == null) {
                    return CompletableFuture.completedFuture(result);
                }
                return this.compensatePriorityAndFail(
                    current, priorDelivery.get(), throwable);
            }).thenCompose(future -> future);
        });
    }

    private CompletableFuture<EditResult> editAuthoritative(
        Parcel parcel,
        UnaryOperator<Parcel> edit
    ) {
        return this.parcelService.serializeParcelOperation(
            parcel.uuid(), () -> this.parcelService
                .getAuthoritativeWithinParcelOperation(parcel.uuid())
                .thenCompose(optional -> {
                    if (optional.isEmpty()) {
                        return parcelUnavailable();
                    }
                    Parcel current = optional.get();
                    Parcel updated = edit.apply(current);
                    return this.parcelService.updateIfStatusWithinParcelOperation(
                        updated, current.status()).thenApply(applied ->
                        Boolean.TRUE.equals(applied)
                            ? EditResult.ok()
                            : EditResult.of(EditResult.Status.PARCEL_COLLECTED));
                }));
    }

    private CompletableFuture<EditResult> compensateAndFail(
        Parcel priorParcel,
        Parcel changedParcel,
        Optional<Delivery> priorDelivery,
        Throwable trigger
    ) {
        Throwable failure = unwrap(trigger);
        return this.attemptCompensation(
                () -> this.parcelService.updateIfStatusWithinParcelOperation(
                    priorParcel, changedParcel.status()),
                "Restore parcel " + priorParcel.uuid(),
                failure
            )
            .thenCompose(ignored -> this.restoreDelivery(
                priorParcel.uuid(), priorDelivery, failure))
            .thenCompose(ignored -> this.rearmPriorDelivery(
                priorParcel, priorDelivery, failure))
            .thenCompose(ignored -> CompletableFuture.failedFuture(failure));
    }

    private CompletableFuture<EditResult> compensatePriorityAndFail(
        Parcel priorParcel,
        Delivery priorDelivery,
        Throwable trigger
    ) {
        Throwable failure = unwrap(trigger);
        return this.attemptCompensation(
                () -> this.parcelService.updateWithinParcelOperation(priorParcel),
                "Restore parcel priority " + priorParcel.uuid(),
                failure
            )
            .thenCompose(ignored -> this.attemptCompensation(
                () -> this.deliveryManager.update(
                    priorParcel.uuid(), priorDelivery.deliveryTimestamp()),
                "Restore delivery " + priorParcel.uuid(),
                failure
            ))
            .thenCompose(ignored -> this.rearmPriorDelivery(
                priorParcel, Optional.of(priorDelivery), failure))
            .thenCompose(ignored -> CompletableFuture.failedFuture(failure));
    }

    private CompletableFuture<Void> restoreDelivery(
        UUID parcel,
        Optional<Delivery> priorDelivery,
        Throwable failure
    ) {
        if (priorDelivery.isPresent()) {
            return this.attemptCompensation(
                () -> this.deliveryManager.update(
                    parcel, priorDelivery.get().deliveryTimestamp()),
                "Restore delivery " + parcel,
                failure);
        }
        return this.attemptCompensation(
            () -> this.deliveryManager.delete(parcel),
            "Delete created delivery " + parcel,
            failure);
    }

    private CompletableFuture<Void> rearmPriorDelivery(
        Parcel priorParcel,
        Optional<Delivery> priorDelivery,
        Throwable failure
    ) {
        if (priorParcel.status() != ParcelStatus.SENT || priorDelivery.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return this.attemptCompensation(() -> {
            this.scheduleSend(
                priorParcel,
                Duration.between(
                    Instant.now(), priorDelivery.get().deliveryTimestamp()));
            return CompletableFuture.completedFuture(null);
        }, "Re-arm delivery task " + priorParcel.uuid(), failure);
    }

    private CompletableFuture<Void> attemptCompensation(
        Supplier<CompletableFuture<?>> action,
        String description,
        Throwable failure
    ) {
        CompletableFuture<?> result;
        try {
            result = action.get();
        } catch (Throwable throwable) {
            failure.addSuppressed(unwrap(throwable));
            return CompletableFuture.completedFuture(null);
        }
        if (result == null) {
            failure.addSuppressed(
                new IllegalStateException(description + " returned a null future"));
            return CompletableFuture.completedFuture(null);
        }
        return result.handle((value, throwable) -> {
            if (throwable != null) {
                failure.addSuppressed(unwrap(throwable));
            } else if (value instanceof Boolean completed && !completed) {
                failure.addSuppressed(
                    new IllegalStateException(description + " returned false"));
            }
            return null;
        });
    }

    private Duration deliveryDelay(boolean priority) {
        return priority
            ? this.config.settings.priorityParcelSendDuration
            : this.config.settings.parcelSendDuration;
    }

    private static CompletableFuture<EditResult> parcelUnavailable() {
        return CompletableFuture.completedFuture(
            EditResult.of(EditResult.Status.PARCEL_COLLECTED));
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException
            || current instanceof java.util.concurrent.ExecutionException)
            && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static <T> CompletableFuture<T> invoke(
        Supplier<CompletableFuture<T>> operation
    ) {
        try {
            return java.util.Objects.requireNonNull(
                operation.get(), "Admin parcel operation returned a null future");
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(unwrap(throwable));
        }
    }

    private static Parcel withName(Parcel p, String name) {
        return new Parcel(p.uuid(), p.sender(), name, p.description(), p.priority(), p.receiver(), p.size(), p.entryLocker(), p.destinationLocker(), p.status());
    }

    private static Parcel withDescription(Parcel p, String description) {
        return new Parcel(p.uuid(), p.sender(), p.name(), description, p.priority(), p.receiver(), p.size(), p.entryLocker(), p.destinationLocker(), p.status());
    }

    private static Parcel withPriority(Parcel p, boolean priority) {
        return new Parcel(p.uuid(), p.sender(), p.name(), p.description(), priority, p.receiver(), p.size(), p.entryLocker(), p.destinationLocker(), p.status());
    }

    private static Parcel withSize(Parcel p, ParcelSize size) {
        return new Parcel(p.uuid(), p.sender(), p.name(), p.description(), p.priority(), p.receiver(), size, p.entryLocker(), p.destinationLocker(), p.status());
    }

    private static Parcel withStatus(Parcel p, ParcelStatus status) {
        return new Parcel(p.uuid(), p.sender(), p.name(), p.description(), p.priority(), p.receiver(), p.size(), p.entryLocker(), p.destinationLocker(), status);
    }

    private static Parcel withReceiver(Parcel p, UUID receiver) {
        return new Parcel(p.uuid(), p.sender(), p.name(), p.description(), p.priority(), receiver, p.size(), p.entryLocker(), p.destinationLocker(), p.status());
    }

    private static Parcel withDestination(Parcel p, UUID destinationLocker) {
        return new Parcel(p.uuid(), p.sender(), p.name(), p.description(), p.priority(), p.receiver(), p.size(), p.entryLocker(), destinationLocker, p.status());
    }
}
