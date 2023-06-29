package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;

import java.util.Set;

public record ParcelPageResult(Set<Parcel> parcels, boolean hasNextPage) {

}
