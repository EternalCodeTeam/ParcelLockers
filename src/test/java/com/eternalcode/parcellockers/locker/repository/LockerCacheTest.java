package com.eternalcode.parcellockers.locker.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LockerCacheTest {

    private LockerCache lockerCache;
    private Locker testLocker;
    private Position testPosition;

    @BeforeEach
    void setUp() {
        this.lockerCache = new LockerCache();
        this.testPosition = new Position(10, 20, 30, "world");
        this.testLocker = new Locker(UUID.randomUUID(), "Test locker", this.testPosition);
    }

    @Test
    void shouldPutAndGetLockerByUuid() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        Optional<Locker> result = this.lockerCache.get(this.testLocker.uuid());

        // then
        assertTrue(result.isPresent());
        assertEquals(this.testLocker, result.get());
    }

    @Test
    void shouldPutAndGetLockerByPosition() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        Optional<Locker> result = this.lockerCache.get(this.testPosition);

        // then
        assertTrue(result.isPresent());
        assertEquals(this.testLocker, result.get());
    }

    @Test
    void shouldRemoveLockerByUuid() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        Locker removed = this.lockerCache.remove(this.testLocker.uuid());

        // then
        assertEquals(this.testLocker, removed);
        assertFalse(this.lockerCache.get(this.testLocker.uuid()).isPresent());
        assertFalse(this.lockerCache.get(this.testPosition).isPresent());
    }

    @Test
    void shouldRemoveLockerByObject() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        Locker removed = this.lockerCache.remove(this.testLocker);

        // then
        assertEquals(this.testLocker, removed);
        assertFalse(this.lockerCache.get(this.testLocker.uuid()).isPresent());
        assertFalse(this.lockerCache.get(this.testPosition).isPresent());
    }

    @Test
    void shouldPutAllLockers() {
        // given
        Locker locker1 = new Locker(UUID.randomUUID(), "Locker 1", new Position(1, 2, 3, "world"));
        Locker locker2 = new Locker(UUID.randomUUID(), "Locker 2", new Position(4, 5, 6, "world"));
        Map<UUID, Locker> lockers = Map.of(
            locker1.uuid(), locker1,
            locker2.uuid(), locker2
        );

        // when
        this.lockerCache.putAll(lockers);

        // then
        assertTrue(this.lockerCache.get(locker1.uuid()).isPresent());
        assertTrue(this.lockerCache.get(locker2.uuid()).isPresent());
        assertTrue(this.lockerCache.get(locker1.position()).isPresent());
        assertTrue(this.lockerCache.get(locker2.position()).isPresent());
    }

    @Test
    void shouldClearCache() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        this.lockerCache.clear();

        // then
        assertFalse(this.lockerCache.get(this.testLocker.uuid()).isPresent());
        assertFalse(this.lockerCache.get(this.testPosition).isPresent());
        assertEquals(0, this.lockerCache.cache().size());
    }

    @Test
    void shouldReturnCorrectSize() {
        // given
        this.lockerCache.put(this.testLocker);

        // when & then
        assertEquals(1, this.lockerCache.cache().size());
        assertEquals(1, this.lockerCache.positionCache().size());
    }

    @Test
    void shouldReturnUnmodifiableMaps() {
        // given
        this.lockerCache.put(this.testLocker);

        // when
        Map<UUID, Locker> cache = this.lockerCache.cache();
        Map<Position, Locker> positionCache = this.lockerCache.positionCache();

        // then
        assertThrows(UnsupportedOperationException.class, () -> cache.clear());
        assertThrows(UnsupportedOperationException.class, () -> positionCache.clear());
    }
}
