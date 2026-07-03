package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.parcellockers.returns.CollectedParcel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CollectedParcelRepository {

    CompletableFuture<Void> save(CollectedParcel collectedParcel);

    CompletableFuture<Optional<CollectedParcel>> find(UUID parcel);

    /** Returns rows collected at or before the given cutoff (i.e. whose return window expired). */
    CompletableFuture<List<CollectedParcel>> findExpired(Instant cutoff);

    CompletableFuture<Boolean> delete(UUID parcel);
}
