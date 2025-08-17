package com.eternalcode.parcellockers.delivery.repository;

import com.eternalcode.parcellockers.database.persister.InstantPersister;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.time.Instant;
import java.util.UUID;

@DatabaseTable(tableName = "deliveries")
class DeliveryTable {

    @DatabaseField(id = true)
    private UUID parcel;

    @DatabaseField(persisterClass = InstantPersister.class)
    private Instant deliveryTimestamp;

    DeliveryTable() {
    }

    DeliveryTable(UUID parcel, Instant deliveryTimestamp) {
        this.parcel = parcel;
        this.deliveryTimestamp = deliveryTimestamp;
    }

    public static DeliveryTable from(Delivery delivery) {
        return new DeliveryTable(delivery.parcel(), delivery.deliveryTimestamp());
    }

    Delivery toDelivery() {
        return new Delivery(this.parcel, this.deliveryTimestamp);
    }
}
