package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LockerRepository {

    CompletableFuture<Locker> save(Locker locker);

    CompletableFuture<Optional<Locker>> find(UUID uuid);

    CompletableFuture<Optional<Locker>> find(Position position);

    CompletableFuture<Integer> delete(UUID uuid);

    CompletableFuture<Integer> delete(Locker locker);

    CompletableFuture<Integer> deleteAll();

    CompletableFuture<PageResult<Locker>> findPage(Page page);
}
