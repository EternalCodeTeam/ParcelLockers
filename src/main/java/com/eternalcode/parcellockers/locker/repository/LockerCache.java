package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class LockerCache {

    private final Cache<@NotNull UUID, Locker> cache;
    private final Cache<@NotNull Position, Locker> positionCache;

    public LockerCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(2))
                .build();

        this.positionCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(2))
                .build();
    }

    public void put(Locker locker) {
        this.cache.put(locker.uuid(), locker);
        this.positionCache.put(locker.position(), locker);
    }

    public void putAll(Map<UUID, Locker> lockers) {
        this.cache.putAll(lockers);
        lockers.values().forEach(locker -> this.positionCache.put(locker.position(), locker));
    }

    public Locker remove(UUID uuid) {
        Locker locker = this.cache.getIfPresent(uuid);
        if (locker != null) {
            this.positionCache.invalidate(locker.position());
            this.cache.invalidate(uuid);
        }
        return locker;
    }

    public Locker remove(Locker locker) {
        this.positionCache.invalidate(locker.position());
        this.cache.invalidate(locker.uuid());
        return locker;
    }

    public Map<UUID, Locker> cache() {
        return Collections.unmodifiableMap(this.cache.asMap());
    }

    public Map<Position, Locker> positionCache() {
        return Collections.unmodifiableMap(this.positionCache.asMap());
    }

    public Optional<Locker> get(UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    public Optional<Locker> get(Position position) {
        return Optional.ofNullable(this.positionCache.getIfPresent(position));
    }

    public void clear() {
        this.cache.invalidateAll();
        this.positionCache.invalidateAll();
    }
}
