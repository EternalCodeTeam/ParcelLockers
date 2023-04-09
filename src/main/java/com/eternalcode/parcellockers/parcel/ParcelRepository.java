package com.eternalcode.parcellockers.parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Optional<Parcel>> findByUuid(UUID uuid);

    CompletableFuture<Set<Parcel>> findAll();
}
