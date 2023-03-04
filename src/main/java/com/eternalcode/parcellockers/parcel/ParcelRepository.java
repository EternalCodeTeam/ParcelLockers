package com.eternalcode.parcellockers.parcel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParcelRepository {

    void save(Parcel parcel);

    Optional<Parcel> findByUuid(UUID uuid);

    List<Parcel> findAll();
}
