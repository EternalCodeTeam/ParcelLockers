package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AdminParcelService {

    private final ParcelService parcelService;
    private final ParcelContentManager parcelContentManager;
    private final DeliveryManager deliveryManager;
    private final LockerManager lockerManager;
    private final PluginConfig config;
    private final Scheduler scheduler;

    public AdminParcelService(ParcelService parcelService, ParcelContentManager parcelContentManager,
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

    private CompletableFuture<EditResult> persist(Parcel updated) {
        return this.parcelService.update(updated).thenApply(ignored -> EditResult.ok());
    }

    public CompletableFuture<EditResult> changeName(Parcel parcel, String name) {
        return this.persist(withName(parcel, name));
    }

    public CompletableFuture<EditResult> changeDescription(Parcel parcel, String description) {
        return this.persist(withDescription(parcel, description));
    }

    public CompletableFuture<EditResult> changeStatus(Parcel parcel, ParcelStatus status) {
        Parcel updated = withStatus(parcel, status);
        return this.parcelService.update(updated).thenCompose(ignored -> {
            if (status == parcel.status()) {
                return CompletableFuture.completedFuture(EditResult.ok());
            }
            return status == ParcelStatus.SENT ? this.armDelivery(updated) : this.disarmDelivery(updated);
        });
    }

    /**
     * A SENT parcel must own a delivery row and a scheduled task, otherwise it would sit in SENT
     * forever (e.g. after an admin flips a DELIVERED parcel back to SENT). Re-arm both.
     */
    private CompletableFuture<EditResult> armDelivery(Parcel parcel) {
        return this.deliveryManager.get(parcel.uuid()).thenCompose(existing -> {
            if (existing.isPresent()) {
                // A delivery row already exists; just make sure a task is armed for it.
                this.scheduleSend(parcel, Duration.between(Instant.now(), existing.get().deliveryTimestamp()));
                return CompletableFuture.completedFuture(EditResult.ok());
            }
            Duration delay = parcel.priority()
                ? this.config.settings.priorityParcelSendDuration
                : this.config.settings.parcelSendDuration;
            return this.deliveryManager.update(parcel.uuid(), Instant.now().plus(delay))
                .thenApply(delivery -> {
                    this.scheduleSend(parcel, delay);
                    return EditResult.ok();
                });
        });
    }

    /** A DELIVERED parcel keeps no pending delivery; drop any stray row so no task fires later. */
    private CompletableFuture<EditResult> disarmDelivery(Parcel parcel) {
        return this.deliveryManager.get(parcel.uuid()).thenCompose(existing -> {
            if (existing.isEmpty()) {
                return CompletableFuture.completedFuture(EditResult.ok());
            }
            return this.deliveryManager.delete(parcel.uuid()).thenApply(deleted -> EditResult.ok());
        });
    }

    private void scheduleSend(Parcel parcel, Duration delay) {
        this.scheduler.runLaterAsync(
            new ParcelSendTask(parcel, this.parcelService, this.deliveryManager, this.scheduler),
            delay.isNegative() ? Duration.ZERO : delay);
    }

    public CompletableFuture<EditResult> changeReceiver(Parcel parcel, UUID receiver) {
        return this.persist(withReceiver(parcel, receiver));
    }

    public CompletableFuture<EditResult> changeSize(Parcel parcel, ParcelSize newSize) {
        return this.parcelContentManager.get(parcel.uuid()).thenCompose(optional -> {
            int itemCount = optional.map(content -> content.items().size()).orElse(0);
            if (itemCount > capacity(newSize)) {
                return CompletableFuture.completedFuture(EditResult.of(EditResult.Status.SIZE_TOO_SMALL));
            }
            return this.persist(withSize(parcel, newSize));
        });
    }

    public CompletableFuture<EditResult> changeDestination(Parcel parcel, UUID destinationLocker) {
        return this.lockerManager.isLockerFull(destinationLocker).thenCompose(full -> {
            if (Boolean.TRUE.equals(full)) {
                return CompletableFuture.completedFuture(EditResult.of(EditResult.Status.DESTINATION_FULL));
            }
            return this.persist(withDestination(parcel, destinationLocker));
        });
    }

    public CompletableFuture<EditResult> changePriority(Parcel parcel, boolean newPriority) {
        Parcel updated = withPriority(parcel, newPriority);
        return this.parcelService.update(updated).thenCompose(ignored -> {
            if (parcel.status() != ParcelStatus.SENT || newPriority == parcel.priority()) {
                return CompletableFuture.completedFuture(EditResult.ok());
            }
            return this.deliveryManager.get(parcel.uuid()).thenCompose(optionalDelivery -> {
                if (optionalDelivery.isEmpty()) {
                    return CompletableFuture.completedFuture(EditResult.ok());
                }
                Instant now = Instant.now();
                Instant shifted = shiftedDeliveryTimestamp(
                    optionalDelivery.get().deliveryTimestamp(),
                    parcel.priority(), newPriority,
                    this.config.settings.parcelSendDuration,
                    this.config.settings.priorityParcelSendDuration,
                    now);
                return this.deliveryManager.update(parcel.uuid(), shifted)
                    .thenApply(ignoredDelivery -> {
                        // The task scheduled at send/startup still points at the old time; arm a fresh
                        // task at the new time so an earlier delivery actually fires earlier. The stale
                        // task self-heals at fire time: it aborts if the parcel is already delivered,
                        // or reschedules itself if it sees the later timestamp first.
                        this.scheduleSend(updated, Duration.between(now, shifted));
                        return EditResult.ok();
                    });
            });
        });
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
