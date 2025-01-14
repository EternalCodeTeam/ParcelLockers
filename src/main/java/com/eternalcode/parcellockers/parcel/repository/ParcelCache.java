package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParcelCache {

    private final Map<UUID, Parcel> cache = new ConcurrentHashMap<>();

    public void put(Parcel parcel) {
        cache.put(parcel.uuid(), parcel);
    }

    public void putAll(Map<UUID, Parcel> parcels) {
        cache.putAll(parcels);
    }

    public Optional<Parcel> get(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }

    public Map<UUID, Parcel> cache() {
        return Collections.unmodifiableMap(cache);
    }
}
