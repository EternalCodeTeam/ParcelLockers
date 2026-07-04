package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CollectedParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements CollectedParcelRepository {

    public CollectedParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(CollectedParcelTable.class);
    }

    @Override
    public CompletableFuture<Void> save(CollectedParcel collectedParcel) {
        Objects.requireNonNull(collectedParcel, "CollectedParcel cannot be null");
        // Upsert, not insert-if-absent: a re-collect after a failed best-effort delete of the
        // previous row must overwrite the stale collectedAt, not silently keep it (which would
        // shorten the new return window by however long the parcel spent on its second trip).
        return this.upsert(CollectedParcelTable.class, CollectedParcelTable.from(collectedParcel))
            .thenApply(status -> null);
    }

    @Override
    public CompletableFuture<Optional<CollectedParcel>> find(UUID parcel) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        return this.selectSafe(CollectedParcelTable.class, parcel)
            .thenApply(optional -> optional.map(CollectedParcelTable::toCollectedParcel));
    }

    @Override
    public CompletableFuture<List<CollectedParcel>> findExpired(Instant cutoff) {
        Objects.requireNonNull(cutoff, "Cutoff cannot be null");
        // collected_at is persisted as an ISO-8601 string (InstantPersister); a SQL range operator
        // would compare it lexicographically, which misorders same-second values with different
        // fractional renderings. Compare temporally in Java instead — the table only holds parcels
        // inside the return window, so a full scan per purge run is cheap.
        return this.selectAll(CollectedParcelTable.class)
            .thenApply(rows -> rows.stream()
                .map(CollectedParcelTable::toCollectedParcel)
                .filter(collected -> !collected.collectedAt().isAfter(cutoff))
                .toList());
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID parcel) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        return this.deleteById(CollectedParcelTable.class, parcel).thenApply(rows -> rows > 0);
    }
}
