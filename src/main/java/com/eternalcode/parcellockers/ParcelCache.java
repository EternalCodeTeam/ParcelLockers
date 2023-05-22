package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcellocker.ParcelLocker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParcelCache {

    private final Set<Parcel> parcels = new HashSet<>();
    private final Set<ParcelLocker> parcelLockers = new HashSet<>();

    public Set<Parcel> getParcels() {
        return this.parcels;
    }

    public Set<ParcelLocker> getParcelLockers() {
        return this.parcelLockers;
    }

    public Parcel findParcel(UUID uuid) {
        return this.parcels.stream()
                .filter(parcel -> parcel.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public ParcelLocker findParcelLocker(UUID uuid) {
        return this.parcelLockers.stream()
                .filter(parcelLocker -> parcelLocker.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

}
