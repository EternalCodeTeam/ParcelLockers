package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.ParcelLocker;
import com.eternalcode.parcellockers.parcel.ParcelLockerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParcelLockerManager {

    private final ParcelLockerRepository repository;

    public ParcelLockerManager(ParcelLockerRepository repository) {
        this.repository = repository;
    }

    // TODO add real methods

    public void save(ParcelLocker parcelLocker) {
        this.repository.save(parcelLocker);
    }

    public Optional<ParcelLocker> findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid).join();
    }

    public void remove(UUID uuid) {
        this.repository.remove(uuid);
    }

    public List<ParcelLocker> findAll() {
        return this.repository.findAll().join();
    }
}
