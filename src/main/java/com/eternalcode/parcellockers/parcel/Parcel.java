package com.eternalcode.parcellockers.parcel;

import java.util.UUID;

public record Parcel(
    UUID uuid,
    UUID sender,
    String name,
    String description,
    boolean priority,
    UUID receiver,
    ParcelSize size,
    UUID entryLocker,
    UUID destinationLocker,
    ParcelStatus status
) {
}
