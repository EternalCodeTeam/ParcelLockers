package com.eternalcode.parcellockers.parcel;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelDestinationTest {

    private static Parcel parcelWithDestination(UUID destinationLocker) {
        return new Parcel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "name",
            "description",
            false,
            UUID.randomUUID(),
            ParcelSize.SMALL,
            UUID.randomUUID(),
            destinationLocker,
            ParcelStatus.DELIVERED
        );
    }

    @Test
    @DisplayName("Should be destined for the locker matching its destination locker")
    void isDestinedForMatchingLocker() {
        UUID locker = UUID.randomUUID();
        Parcel parcel = parcelWithDestination(locker);

        assertTrue(parcel.isDestinedFor(locker));
    }

    @Test
    @DisplayName("Should not be destined for a locker other than its destination locker")
    void isDestinedForOtherLocker() {
        Parcel parcel = parcelWithDestination(UUID.randomUUID());

        assertFalse(parcel.isDestinedFor(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should not be destined for any locker when its destination locker is null")
    void isDestinedForWhenDestinationIsNull() {
        Parcel parcel = parcelWithDestination(null);

        assertFalse(parcel.isDestinedFor(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should not be destined for a null locker")
    void isDestinedForNullLocker() {
        Parcel parcel = parcelWithDestination(UUID.randomUUID());

        assertFalse(parcel.isDestinedFor(null));
    }
}
