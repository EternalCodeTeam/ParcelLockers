package com.eternalcode.parcellockers.locker.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        lockerCache = new LockerCache();
        testPosition = new Position(10, 20, 30, "world");
        testLocker = new Locker(UUID.randomUUID(), "Test locker", testPosition);
    }

    @Test
    void shouldPutAndGetLockerByUuid() {
        // given
        lockerCache.put(testLocker);

        // when
        Optional<Locker> result = lockerCache.get(testLocker.uuid());

        // then
        assertTrue(result.isPresent());
        assertEquals(testLocker, result.get());
    }

    @Test
    void shouldPutAndGetLockerByPosition() {
        // given
        lockerCache.put(testLocker);

        // when
        Optional<Locker> result = lockerCache.get(testPosition);

        // then
        assertTrue(result.isPresent());
        assertEquals(testLocker, result.get());
    }

    @Test
    void shouldRemoveLockerByUuid() {
        // given
        lockerCache.put(testLocker);

        // when
        Locker removed = lockerCache.remove(testLocker.uuid());

        // then
        assertEquals(testLocker, removed);
        assertFalse(lockerCache.get(testLocker.uuid()).isPresent());
        assertFalse(lockerCache.get(testPosition).isPresent());
    }

    @Test
    void shouldRemoveLockerByObject() {
        // given
        lockerCache.put(testLocker);

        // when
        Locker removed = lockerCache.remove(testLocker);

        // then
        assertEquals(testLocker, removed);
        assertFalse(lockerCache.get(testLocker.uuid()).isPresent());
        assertFalse(lockerCache.get(testPosition).isPresent());
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
        lockerCache.putAll(lockers);

        // then
        assertTrue(lockerCache.get(locker1.uuid()).isPresent());
        assertTrue(lockerCache.get(locker2.uuid()).isPresent());
        assertTrue(lockerCache.get(locker1.position()).isPresent());
        assertTrue(lockerCache.get(locker2.position()).isPresent());
    }

    @Test
    void shouldClearCache() {
        // given
        lockerCache.put(testLocker);

        // when
        lockerCache.clear();

        // then
        assertFalse(lockerCache.get(testLocker.uuid()).isPresent());
        assertFalse(lockerCache.get(testPosition).isPresent());
        assertEquals(0, lockerCache.cache().size());
    }

    @Test
    void shouldReturnCorrectSize() {
        // given
        lockerCache.put(testLocker);

        // when & then
        assertEquals(1, lockerCache.cache().size());
        assertEquals(1, lockerCache.positionCache().size());
    }

    @Test
    void shouldReturnUnmodifiableMaps() {
        // given
        lockerCache.put(testLocker);

        // when
        Map<UUID, Locker> cache = lockerCache.cache();
        Map<Position, Locker> positionCache = lockerCache.positionCache();

        // then
        assertThrows(UnsupportedOperationException.class, () -> cache.clear());
        assertThrows(UnsupportedOperationException.class, () -> positionCache.clear());
    }
}
