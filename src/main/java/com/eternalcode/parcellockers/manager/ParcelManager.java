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

    // TODO add real methods

    public CompletableFuture<Void> save(Parcel parcel) {
        return this.repository.save(parcel);
    }

    public CompletableFuture<Void> remove(UUID uuid) {
        return this.repository.remove(uuid);
    }

    public CompletableFuture<Optional<Parcel>> findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid);
    }

    public CompletableFuture<Set<Parcel>> findAll() {
        return this.repository.findAll();
    }

}
