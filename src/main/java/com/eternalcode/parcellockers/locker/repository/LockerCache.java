package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LockerCache {

    private final Map<UUID, Locker> cache = new ConcurrentHashMap<>();
    private final Map<Position, Locker> positionCache = new ConcurrentHashMap<>();

    public void put(Locker locker) {
        this.cache.put(locker.uuid(), locker);
        this.positionCache.put(locker.position(), locker);
    }

    public void putAll(Map<UUID, Locker> lockers) {
        this.cache.putAll(lockers);
        lockers.values().forEach(locker -> this.positionCache.put(locker.position(), locker));
    }

    public Locker remove(UUID uuid) {
        this.positionCache.remove(this.cache.get(uuid).position());
        return this.cache.remove(uuid);
    }

    public Locker remove(Locker locker) {
        this.positionCache.remove(locker.position());
        return this.cache.remove(locker.uuid());
    }

    public Map<UUID, Locker> cache() {
        return Collections.unmodifiableMap(this.cache);
    }

    public Map<Position, Locker> positionCache() {
        return Collections.unmodifiableMap(this.positionCache);
    }

    public Optional<Locker> get(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    public Optional<Locker> get(Position position) {
        return Optional.ofNullable(this.positionCache.get(position));
    }

    public void clear() {
        this.cache.clear();
        this.positionCache.clear();
    }
}
