package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;

public class LockerManager {

    private final LockerRepositoryImpl lockerRepository;

    public LockerManager(LockerRepositoryImpl lockerRepository) {
        this.lockerRepository = lockerRepository;
    }
}
