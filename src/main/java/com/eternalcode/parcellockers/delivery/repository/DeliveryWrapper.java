package com.eternalcode.parcellockers.delivery.repository;

import com.eternalcode.parcellockers.delivery.Delivery;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "deliveries")
class DeliveryWrapper {

    @DatabaseField(id = true)
    private UUID parcel;

    @DatabaseField
    private long deliveryTimestamp;

    DeliveryWrapper() {
    }

    DeliveryWrapper(UUID parcel, long deliveryTimestamp) {
        this.parcel = parcel;
        this.deliveryTimestamp = deliveryTimestamp;
    }

    public static DeliveryWrapper from(Delivery delivery) {
        return new DeliveryWrapper(delivery.parcel(), delivery.deliveryTimestamp());
    }

    Delivery toDelivery() {
        return new Delivery(this.parcel, this.deliveryTimestamp);
    }
}
