package com.eternalcode.parcellockers.parcellocker;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Position;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParcelLocker {

    private final UUID uuid;
    private final String description;
    private final Set<Parcel> parcels;
    private final Position position;

    public ParcelLocker(UUID uuid, String description, Position position) {
        this.uuid = uuid;
        this.description = description;
        this.parcels = new HashSet<>();
        this.position = position;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<Parcel> getParcels() {
        return this.parcels;
    }

    public Position getPosition() {
        return this.position;
    }

    public void addParcel(Parcel parcel) {
        this.parcels.add(parcel);
    }

    public void removeParcel(Parcel parcel) {
        this.parcels.remove(parcel);
    }
}
