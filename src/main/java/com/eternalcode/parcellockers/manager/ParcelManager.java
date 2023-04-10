package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepository;

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

    public CompletableFuture<Void> remove(UUID uuid) {
        return this.repository.remove(uuid);
    }

    public Optional<Parcel> findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid).join();
    }

    public Set<Parcel> findAll() {
        return this.repository.findAll().join();
    }



}
