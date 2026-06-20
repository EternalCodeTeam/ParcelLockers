package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelDeliverEvent;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ParcelSendTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(ParcelSendTask.class.getName());

    private final Parcel parcel;
    private final ParcelService parcelService;
    private final DeliveryManager deliveryManager;

    public ParcelSendTask(
        Parcel parcel,
        ParcelService parcelService,
        DeliveryManager deliveryManager
    ) {
        this.parcel = parcel;
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
    }

    @Override
    public void run() {
        Parcel updated = new Parcel(
            this.parcel.uuid(),
            this.parcel.sender(),
            this.parcel.name(),
            this.parcel.description(),
            this.parcel.priority(),
            this.parcel.receiver(),
            this.parcel.size(),
            this.parcel.entryLocker(),
            this.parcel.destinationLocker(),
            ParcelStatus.DELIVERED
        );

        // Fire ParcelDeliverEvent
        ParcelDeliverEvent event = new ParcelDeliverEvent(updated);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            LOGGER.info("ParcelDeliverEvent was cancelled for parcel " + updated.uuid());
            return;
        }

        // Delete the delivery only after the status update succeeds. If the update fails, the
        // delivery row is left intact so the task is rescheduled on the next startup; deleting it
        // unconditionally could strand the parcel in SENT with no delivery, never to be delivered.
        this.parcelService.update(updated)
            .thenCompose(ignored -> this.deliveryManager.delete(updated.uuid()))
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to deliver parcel " + updated.uuid() + " (delivery left for retry): " + throwable.getMessage());
                return null;
            });
    }

}
