package com.eternalcode.parcellockers.parcellocker.repository;

import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.shared.Position;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelLockerRepository {

    CompletableFuture<Void> save(ParcelLocker parcelLocker);

    CompletableFuture<Set<ParcelLocker>> findAll();

    CompletableFuture<Optional<ParcelLocker>> findByUUID(UUID uuid);

    CompletableFuture<Optional<ParcelLocker>> findByPosition(Position position);

    CompletableFuture<Void> remove(UUID uuid);

    CompletableFuture<Void> remove(ParcelLocker parcelLocker);

    CompletableFuture<Set<ParcelLocker>> findPage(int page, int pageSize);
}
