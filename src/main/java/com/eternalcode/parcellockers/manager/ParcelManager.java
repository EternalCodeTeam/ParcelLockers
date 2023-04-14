package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.ParcelRepository;

public class ParcelManager {

    private final ParcelRepository repository;

    public ParcelManager(ParcelRepository repository) {
        this.repository = repository;
    }

}
