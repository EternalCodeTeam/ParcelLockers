package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelDeliverEvent;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ParcelSendTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(ParcelSendTask.class.getName());

    public enum Decision { DELIVER, RESCHEDULE, ABORT }

    private final UUID parcelId;
    private final ParcelService parcelService;
    private final DeliveryManager deliveryManager;
    private final Scheduler scheduler;

    public ParcelSendTask(Parcel parcel, ParcelService parcelService, DeliveryManager deliveryManager, Scheduler scheduler) {
        this.parcelId = parcel.uuid();
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
        this.scheduler = scheduler;
    }

    /** Pure decision: what to do given the latest parcel + delivery state at fire time. */
    public static Decision decide(Optional<Parcel> currentParcel, Optional<Delivery> currentDelivery, Instant now) {
        if (currentParcel.isEmpty() || currentParcel.get().status() == ParcelStatus.DELIVERED) {
            return Decision.ABORT;
        }
        if (currentDelivery.isPresent() && currentDelivery.get().deliveryTimestamp().isAfter(now)) {
            return Decision.RESCHEDULE;
        }
        return Decision.DELIVER;
    }

    @Override
    public void run() {
        this.parcelService.get(this.parcelId).thenCompose(optionalParcel ->
            this.deliveryManager.get(this.parcelId).thenAccept(optionalDelivery -> {
                Instant now = Instant.now();
                switch (decide(optionalParcel, optionalDelivery, now)) {
                    case ABORT ->
                        // Parcel gone or already delivered: clean up any stray delivery row.
                        optionalDelivery.ifPresent(delivery -> this.deliveryManager.delete(this.parcelId));
                    case RESCHEDULE -> {
                        Duration remaining = Duration.between(now, optionalDelivery.get().deliveryTimestamp());
                        // Reschedule a fresh task; this instance ends after this run.
                        this.scheduler.runLaterAsync(
                            new ParcelSendTask(optionalParcel.get(), this.parcelService, this.deliveryManager, this.scheduler),
                            remaining.isNegative() ? Duration.ZERO : remaining);
                    }
                    case DELIVER -> this.deliver(optionalParcel.get());
                }
            })).exceptionally(throwable -> {
                LOGGER.severe("ParcelSendTask failed for " + this.parcelId + ": " + throwable.getMessage());
                return null;
            });
    }

    private void deliver(Parcel current) {
        Parcel delivered = new Parcel(current.uuid(), current.sender(), current.name(), current.description(),
            current.priority(), current.receiver(), current.size(), current.entryLocker(),
            current.destinationLocker(), ParcelStatus.DELIVERED);

        ParcelDeliverEvent event = new ParcelDeliverEvent(delivered);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            LOGGER.info("ParcelDeliverEvent was cancelled for parcel " + delivered.uuid());
            return;
        }

        this.parcelService.update(delivered)
            .thenCompose(ignored -> this.deliveryManager.delete(delivered.uuid()))
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to deliver parcel " + delivered.uuid()
                    + " (delivery left for retry): " + throwable.getMessage());
                return null;
            });
    }
}
