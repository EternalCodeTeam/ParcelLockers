package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LockerRepository {

    CompletableFuture<Void> save(Locker locker);

    CompletableFuture<List<Locker>> findAll();

    CompletableFuture<Optional<Locker>> find(UUID uuid);

    CompletableFuture<Optional<Locker>> find(Position position);

    CompletableFuture<Integer> remove(UUID uuid);

    CompletableFuture<Integer> remove(Locker locker);

    CompletableFuture<Integer> removeAll();

    CompletableFuture<LockerPageResult> findPage(Page page);
}
