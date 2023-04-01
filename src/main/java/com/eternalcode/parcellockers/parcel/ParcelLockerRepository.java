package com.eternalcode.parcellockers.parcel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParcelLockerRepository {

    void save(ParcelLocker parcelLocker);

    Optional<ParcelLocker> findByUuid(UUID uuid);

    List<ParcelLocker> findAll();

}
