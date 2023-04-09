package com.eternalcode.parcellockers.parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelManager {

    private final ParcelRepository repository;

    public ParcelManager(ParcelRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<Void> save(Parcel parcel) {
        return this.repository.save(parcel);
    }

    public CompletableFuture<Void> remove(Parcel parcel) {
        return this.repository.remove(parcel);
    }

    public Parcel findByUuid(UUID uuid) {
        Optional<Parcel> result = this.repository.findByUuid(uuid).join();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Parcel with UUID " + uuid + " does not exist!");
        }
        return result.get();
    }

    public Set<Parcel> findAll() {
        return this.repository.findAll().join();
    }



}
