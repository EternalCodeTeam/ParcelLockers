package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;

public class ParcelManager {

    private final ParcelDatabaseService databaseService;
    private final ParcelLockerDatabaseService parcelLockerDatabaseService;

    public ParcelManager(ParcelDatabaseService databaseService, ParcelLockerDatabaseService parcelLockerDatabaseService) {
        this.databaseService = databaseService;
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
    }


}
