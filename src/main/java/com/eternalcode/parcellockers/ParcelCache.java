package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelLocker;

import java.util.HashSet;
import java.util.Set;

public class ParcelCache {

    private final Set<Parcel> parcels = new HashSet<>();
    private final Set<ParcelLocker> parcelLockers = new HashSet<>();

    public Set<Parcel> getParcels() {
        return this.parcels;
    }

    public Set<ParcelLocker> getParcelLockers() {
        return this.parcelLockers;
    }

}
