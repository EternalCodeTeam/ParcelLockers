package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LockerService {

    CompletableFuture<Optional<Locker>> get(UUID uniqueId);

    CompletableFuture<Optional<Locker>> get(Position position);

    CompletableFuture<PageResult<Locker>> get(Page page);

    CompletableFuture<Locker> create(UUID uniqueId, String name, Position position, UUID playerUuid);

    CompletableFuture<Void> delete(UUID uniqueId, UUID playerUuid);

    CompletableFuture<Locker> rename(UUID uniqueId, String newName);

    CompletableFuture<Boolean> isLockerFull(UUID uniqueId);
}
