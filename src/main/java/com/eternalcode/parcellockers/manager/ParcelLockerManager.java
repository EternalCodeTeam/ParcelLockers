package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.ParcelLocker;
import com.eternalcode.parcellockers.parcel.ParcelLockerRepository;

import java.util.List;
import java.util.UUID;

public class ParcelLockerManager {

    private final ParcelLockerRepository repository;

    public ParcelLockerManager(ParcelLockerRepository repository) {
        this.repository = repository;
    }

    public void save(ParcelLocker parcelLocker) {
        this.repository.save(parcelLocker);
    }

    public ParcelLocker findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid).join().orElseThrow(() -> new IllegalArgumentException("ParcelLocker with UUID " + uuid + " does not exist!"));
    }

    public void remove(ParcelLocker parcelLocker) {
        this.repository.remove(parcelLocker);
    }

    public void remove(UUID uuid) {
        this.repository.remove(uuid);
    }

    public List<ParcelLocker> findAll() {
        return this.repository.findAll().join();
    }
}
