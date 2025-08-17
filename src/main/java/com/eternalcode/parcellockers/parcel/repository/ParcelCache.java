package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ParcelCache {

    private final Cache<UUID, Parcel> cache;

    public ParcelCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(2))
                .build();
    }

    public void put(Parcel parcel) {
        this.cache.put(parcel.uuid(), parcel);
    }

    public void putAll(Map<UUID, Parcel> parcels) {
        this.cache.putAll(parcels);
    }

    public Optional<Parcel> get(UUID uuid) {
        return Optional.ofNullable(this.cache.getIfPresent(uuid));
    }

    public Parcel remove(UUID uuid) {
        Parcel parcel = this.cache.getIfPresent(uuid);
        if (parcel != null) {
            this.cache.invalidate(uuid);
        }
        return parcel;
    }

    public void clear() {
        this.cache.invalidateAll();
    }

    public Map<UUID, Parcel> cache() {
        return Collections.unmodifiableMap(this.cache.asMap());
    }
}
