package com.eternalcode.parcellockers.parcel;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Parcel(
    UUID uuid,
    UUID sender,
    String name,
    @Nullable String description,
    boolean priority,
    UUID receiver,
    ParcelSize size,
    UUID entryLocker,
    UUID destinationLocker,
    ParcelStatus status
) {
}
