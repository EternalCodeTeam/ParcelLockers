package com.eternalcode.parcellockers.parcellocker;

import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;

public class ParcelLockerManager {

    private final ParcelLockerRepository parcelLockerRepository;

    public ParcelLockerManager(ParcelLockerRepository databaseService) {
        this.parcelLockerRepository = databaseService;
    }
}
