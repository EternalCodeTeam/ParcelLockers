package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.shared.Position;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelLockerRepository {

    CompletableFuture<Void> save(ParcelLocker parcelLocker);

    CompletableFuture<Optional<ParcelLocker>> findByUuid(UUID uuid);

    CompletableFuture<Optional<ParcelLocker>> findByPosition(Position position);

    CompletableFuture<List<ParcelLocker>> findAll();

    CompletableFuture<Void> remove(ParcelLocker parcelLocker);

    CompletableFuture<Void> remove(UUID uuid);

}
