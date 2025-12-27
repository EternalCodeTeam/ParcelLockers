package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.util.logging.Logger;
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

        this.parcelService.update(updated)
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to update parcel " + updated.uuid() + " to DELIVERED status: " + throwable.getMessage());
                return null;
            });

        this.deliveryManager.delete(updated.uuid())
            .exceptionally(throwable -> {
                LOGGER.warning("Failed to delete delivery for parcel " + updated.uuid() + ": " + throwable.getMessage());
                return null;
            });
    }

}
