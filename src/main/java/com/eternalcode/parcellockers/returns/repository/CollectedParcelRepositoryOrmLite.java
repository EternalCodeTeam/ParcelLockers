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
        return this.insertIfAbsent(CollectedParcelTable.class, CollectedParcelTable.from(collectedParcel))
            .thenApply(dao -> null);
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
        return this.action(CollectedParcelTable.class, dao -> dao.queryBuilder()
            .where()
            .le(CollectedParcelTable.COLLECTED_AT_COLUMN, cutoff)
            .query()
            .stream()
            .map(CollectedParcelTable::toCollectedParcel)
            .toList());
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID parcel) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        return this.deleteById(CollectedParcelTable.class, parcel).thenApply(rows -> rows > 0);
    }
}
