package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Set;
import java.util.UUID;

@DatabaseTable(tableName = "parcels")
class ParcelWrapper {

    @DatabaseField(columnName = "uuid", id = true)
    private UUID uuid;

    @DatabaseField(columnName = "sender", index = true)
    private UUID sender;

    @DatabaseField(columnName = "name")
    private String name;

    @DatabaseField(columnName = "description")
    private String description;

    @DatabaseField(columnName = "priority")
    private boolean priority;

    @DatabaseField(columnName = "receiver", index = true)
    private UUID receiver;

    @DatabaseField(columnName = "size")
    private ParcelSize size;

    @DatabaseField(columnName = "entryLocker", index = true)
    private UUID entryLocker;

    @DatabaseField(columnName = "destinationLocker", index = true)
    private UUID destinationLocker;
    ParcelWrapper() {
    }

    ParcelWrapper(UUID uuid, UUID sender, String name, String description, boolean priority, UUID receiver, ParcelSize size, UUID entryLocker, UUID destinationLocker) {
        this.uuid = uuid;
        this.sender = sender;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.receiver = receiver;
        this.size = size;
        this.entryLocker = entryLocker;
        this.destinationLocker = destinationLocker;
    }

    static ParcelWrapper from(Parcel parcel) {
        return new ParcelWrapper(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(), parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(), parcel.destinationLocker());
    }

    Parcel toParcel() {
        return new Parcel(this.uuid, this.sender, this.name, this.description, this.priority, Set.of(this.sender), this.receiver, this.size, this.entryLocker, this.destinationLocker);
    }
}
