package com.eternalcode.parcellockers.parcel.task;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import org.bukkit.scheduler.BukkitRunnable;

public class ParcelSendTask extends BukkitRunnable {

    private final Parcel parcel;
    private final Delivery delivery;
    private final ParcelRepository parcelRepository;
    private final DeliveryRepository deliveryRepository;
    private final PluginConfiguration config;

    public ParcelSendTask(Parcel parcel, Delivery delivery, ParcelRepository parcelRepository, DeliveryRepository deliveryRepository, PluginConfiguration config) {
        this.parcel = parcel;
        this.delivery = delivery;
        this.parcelRepository = parcelRepository;
        this.deliveryRepository = deliveryRepository;
        this.config = config;
    }

    @Override
    public void run() {
        Parcel updatedParcel = new Parcel(
            parcel.uuid(),
            parcel.sender(),
            parcel.name(),
            parcel.description(),
            parcel.priority(),
            parcel.receiver(),
            parcel.size(),
            parcel.entryLocker(),
            parcel.destinationLocker(),
            ParcelStatus.DELIVERED);

        System.out.println("ParcelSendTask: " + updatedParcel);

        this.parcelRepository.update(updatedParcel);
        this.deliveryRepository.remove(updatedParcel.uuid());
    }

}
