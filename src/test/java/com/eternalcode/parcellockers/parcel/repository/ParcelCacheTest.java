package com.eternalcode.parcellockers.parcel.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelCacheTest {

    private ParcelCache parcelCache;
    private Parcel testParcel;

    @BeforeEach
    void setUp() {
        this.parcelCache = new ParcelCache();
        this.testParcel = new Parcel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Package",
                "Test package name",
                false,
                UUID.randomUUID(),
                ParcelSize.MEDIUM,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ParcelStatus.PENDING
        );
    }

    @Test
    void shouldPutAndGetParcel() {
        // given
        this.parcelCache.put(this.testParcel);

        // when
        Optional<Parcel> result = this.parcelCache.get(this.testParcel.uuid());

        // then
        assertTrue(result.isPresent());
        assertEquals(this.testParcel, result.get());
    }

    @Test
    void shouldReturnEmptyWhenParcelNotFound() {
        // when
        Optional<Parcel> result = this.parcelCache.get(UUID.randomUUID());

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldRemoveParcel() {
        // given
        this.parcelCache.put(this.testParcel);

        // when
        Parcel removed = this.parcelCache.remove(this.testParcel.uuid());

        // then
        assertEquals(this.testParcel, removed);
        assertFalse(this.parcelCache.get(this.testParcel.uuid()).isPresent());
    }

    @Test
    void shouldReturnNullWhenRemovingNonExistentParcel() {
        // when
        Parcel removed = this.parcelCache.remove(UUID.randomUUID());

        // then
        assertNull(removed);
    }

    @Test
    void shouldPutAllParcels() {
        // given
        Parcel parcel1 = new Parcel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Package 1",
                "Description 1",
                true,
                UUID.randomUUID(),
                ParcelSize.SMALL,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ParcelStatus.PENDING
        );

        Parcel parcel2 = new Parcel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Package 2",
                "Description 2",
                false,
                UUID.randomUUID(),
                ParcelSize.LARGE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ParcelStatus.DELIVERED
        );

        Map<UUID, Parcel> parcels = Map.of(
                parcel1.uuid(), parcel1,
                parcel2.uuid(), parcel2
        );

        // when
        this.parcelCache.putAll(parcels);

        // then
        assertTrue(this.parcelCache.get(parcel1.uuid()).isPresent());
        assertTrue(this.parcelCache.get(parcel2.uuid()).isPresent());
        assertEquals(2, this.parcelCache.cache().size());
    }

    @Test
    void shouldClearCache() {
        // given
        this.parcelCache.put(this.testParcel);

        // when
        this.parcelCache.clear();

        // then
        assertFalse(this.parcelCache.get(this.testParcel.uuid()).isPresent());
        assertEquals(0, this.parcelCache.cache().size());
    }

    @Test
    void shouldReturnCorrectSize() {
        // given
        this.parcelCache.put(this.testParcel);

        // when & then
        assertEquals(1, this.parcelCache.cache().size());
    }

    @Test
    void shouldReturnUnmodifiableMap() {
        // given
        this.parcelCache.put(this.testParcel);

        // when
        Map<UUID, Parcel> cache = this.parcelCache.cache();

        // then
        assertThrows(UnsupportedOperationException.class, () -> cache.clear());
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey(this.testParcel.uuid()));
    }

    @Test
    void shouldWorkWithCustomConfiguration() {
        // given
        ParcelCache cache = new ParcelCache();

        // when
        cache.put(this.testParcel);

        // then
        assertTrue(cache.get(this.testParcel.uuid()).isPresent());
        assertEquals(1, cache.cache().size());
    }
}
