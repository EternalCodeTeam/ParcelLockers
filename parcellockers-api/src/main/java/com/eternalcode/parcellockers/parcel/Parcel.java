package com.eternalcode.parcellockers.parcel;

import java.util.Objects;
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

    public Parcel {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(entryLocker, "entryLocker");
        Objects.requireNonNull(destinationLocker, "destinationLocker");
        Objects.requireNonNull(status, "status");
    }
}
