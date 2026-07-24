package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelDeliverEvent;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ParcelSendTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(ParcelSendTask.class.getName());

    public enum Decision { DELIVER, RESCHEDULE, ABORT }

    private final UUID parcelId;
    private final PluginParcelService parcelService;
    private final DeliveryManager deliveryManager;
    private final Scheduler scheduler;

    public ParcelSendTask(Parcel parcel, PluginParcelService parcelService, DeliveryManager deliveryManager, Scheduler scheduler) {
        this.parcelId = parcel.uuid();
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
        this.scheduler = scheduler;
    }

    /** Pure decision: what to do given the latest parcel + delivery state at fire time. */
    public static Decision decide(Optional<Parcel> currentParcel, Optional<Delivery> currentDelivery, Instant now) {
        if (currentParcel.isEmpty() || currentParcel.get().status() != ParcelStatus.SENT) {
            return Decision.ABORT;
        }
        if (currentDelivery.isPresent() && currentDelivery.get().deliveryTimestamp().isAfter(now)) {
            return Decision.RESCHEDULE;
        }
        return Decision.DELIVER;
    }

    @Override
    public void run() {
        this.parcelService.serializeParcelOperation(this.parcelId, this::runWithinParcelOperation)
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "ParcelSendTask failed for " + this.parcelId, throwable);
                return null;
            });
    }

    private java.util.concurrent.CompletableFuture<Void> runWithinParcelOperation() {
        return this.parcelService.getAuthoritativeWithinParcelOperation(this.parcelId)
            .thenCompose(optionalParcel ->
            this.deliveryManager.get(this.parcelId).thenCompose(optionalDelivery -> {
                Instant now = Instant.now();
                return switch (decide(optionalParcel, optionalDelivery, now)) {
                    case ABORT -> {
                        // Parcel gone or already delivered: clean up any stray delivery row.
                        if (optionalDelivery.isEmpty()) {
                            yield java.util.concurrent.CompletableFuture.completedFuture(null);
                        }
                        yield this.deliveryManager.delete(this.parcelId).thenAccept(deleted -> {});
                    }
                    case RESCHEDULE -> {
                        Duration remaining = Duration.between(now, optionalDelivery.get().deliveryTimestamp());
                        // Reschedule a fresh task; this instance ends after this run.
                        this.scheduler.runLaterAsync(
                            new ParcelSendTask(optionalParcel.get(), this.parcelService, this.deliveryManager, this.scheduler),
                            remaining.isNegative() ? Duration.ZERO : remaining);
                        yield java.util.concurrent.CompletableFuture.completedFuture(null);
                    }
                    case DELIVER -> this.deliver(optionalParcel.get());
                };
            }));
    }

    private java.util.concurrent.CompletableFuture<Void> deliver(Parcel current) {
        Parcel delivered = new Parcel(current.uuid(), current.sender(), current.name(), current.description(),
            current.priority(), current.receiver(), current.size(), current.entryLocker(),
            current.destinationLocker(), ParcelStatus.DELIVERED);

        ParcelDeliverEvent event = new ParcelDeliverEvent(delivered);
        this.parcelService.runParcelOperationCallback(
            this.parcelId, () -> Bukkit.getPluginManager().callEvent(event));
        if (event.isCancelled()) {
            LOGGER.info("ParcelDeliverEvent was cancelled for parcel " + delivered.uuid());
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        return this.parcelService.updateWithinParcelOperation(delivered)
            .thenCompose(ignored -> this.deliveryManager.delete(delivered.uuid()))
            .thenAccept(deleted -> {});
    }
}
