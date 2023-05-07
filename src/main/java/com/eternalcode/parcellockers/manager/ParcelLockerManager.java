package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;

public class ParcelLockerManager {

    private final ParcelLockerDatabaseService databaseService;

    public ParcelLockerManager(ParcelLockerDatabaseService databaseService) {
        this.databaseService = databaseService;
    }
}
