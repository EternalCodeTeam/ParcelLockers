package com.eternalcode.parcellockers.parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    Optional<Parcel> findByUuid(UUID uuid);

    Set<Parcel> findAll();
}
