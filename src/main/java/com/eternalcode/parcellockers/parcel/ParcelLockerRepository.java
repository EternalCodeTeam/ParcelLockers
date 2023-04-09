package com.eternalcode.parcellockers.parcel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelLockerRepository {

    CompletableFuture<Void> save(ParcelLocker parcelLocker);

    CompletableFuture<Optional<ParcelLocker>> findByUuid(UUID uuid);

    CompletableFuture<List<ParcelLocker>> findAll();

}
