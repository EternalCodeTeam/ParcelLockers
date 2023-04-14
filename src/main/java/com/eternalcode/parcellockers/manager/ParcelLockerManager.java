package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.ParcelLockerRepository;

public class ParcelLockerManager {

    private final ParcelLockerRepository repository;

    public ParcelLockerManager(ParcelLockerRepository repository) {
        this.repository = repository;
    }

}
