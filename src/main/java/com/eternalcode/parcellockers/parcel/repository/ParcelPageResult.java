package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.List;

public record ParcelPageResult(List<Parcel> parcels, boolean hasNextPage) {

    public static ParcelPageResult empty() {
        return new ParcelPageResult(List.of(), false);
    }
}
