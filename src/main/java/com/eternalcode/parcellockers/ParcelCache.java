package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelLocker;

import java.util.ArrayList;
import java.util.List;

public class ParcelCache {

    private final List<Parcel> parcels = new ArrayList<>();
    private final List<ParcelLocker> parcelLockers = new ArrayList<>();

    public List<Parcel> getParcels() {
        return this.parcels;
    }

    public List<ParcelLocker> getParcelLockers() {
        return this.parcelLockers;
    }

}
