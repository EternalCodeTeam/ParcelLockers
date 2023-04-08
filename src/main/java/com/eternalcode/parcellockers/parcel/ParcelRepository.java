package com.eternalcode.parcellockers.parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ParcelRepository {

    void save(Parcel parcel);

    Optional<Parcel> findByUuid(UUID uuid);

    Set<Parcel> findAll();
}
