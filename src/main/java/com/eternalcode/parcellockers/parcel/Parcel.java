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

    /**
     * Checks whether this parcel is destined for (and therefore collectible from) the given locker.
     *
     * @param locker the locker the player is currently interacting with
     * @return {@code true} only when the locker matches this parcel's destination locker
     */
    public boolean isDestinedFor(UUID locker) {
        return locker != null && locker.equals(this.destinationLocker);
    }
}
