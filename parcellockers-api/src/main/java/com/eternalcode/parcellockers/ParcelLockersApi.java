package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;

public interface ParcelLockersApi {

    ParcelService getParcelService();

    LockerService getLockerService();
}
