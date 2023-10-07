package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.locker.repository.LockerRepository;

public class LockerManager {

    private final LockerRepository lockerRepository;

    public LockerManager(LockerRepository databaseService) {
        this.lockerRepository = databaseService;
    }
}
