package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LockerRepository {

    CompletableFuture<Void> save(Locker locker);

    CompletableFuture<List<Locker>> findAll();

    CompletableFuture<Optional<Locker>> findByUUID(UUID uuid);

    CompletableFuture<Optional<Locker>> findByPosition(Position position);

    CompletableFuture<Integer> remove(UUID uuid);

    CompletableFuture<Integer> remove(Locker locker);

    CompletableFuture<LockerPageResult> findPage(Page page);

    Optional<Locker> getFromCache(UUID uuid);

    Map<UUID, Locker> cache();

    Map<Position, UUID> positionCache();

    boolean isInCache(Position position);

    boolean isInCache(UUID uuid);
}
