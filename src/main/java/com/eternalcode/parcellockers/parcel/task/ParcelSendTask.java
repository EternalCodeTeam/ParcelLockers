package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import org.bukkit.scheduler.BukkitRunnable;

public class ParcelSendTask extends BukkitRunnable {

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


        this.parcelService.update(updated);
        this.deliveryManager.delete(updated.uuid());
    }

}
