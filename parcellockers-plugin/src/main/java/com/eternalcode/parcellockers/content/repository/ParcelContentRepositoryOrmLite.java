package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelContentRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelContentRepository {

    public ParcelContentRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(ParcelContentTable.class);
    }

    @Override
    public CompletableFuture<Void> save(ParcelContent parcelContent) {
        return this.insertIfAbsent(ParcelContentTable.class, ParcelContentTable.from(parcelContent)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> update(ParcelContent parcelContent) {
        return this.upsert(ParcelContentTable.class, ParcelContentTable.from(parcelContent)).thenApply(status -> null);
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uniqueId) {
        return this.deleteById(ParcelContentTable.class, uniqueId).thenApply(i -> i > 0);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ParcelContentTable.class);
    }

    @Override
    public CompletableFuture<Optional<ParcelContent>> find(UUID uniqueId) {
        return this.selectSafe(ParcelContentTable.class, uniqueId)
            .thenApply(optional -> optional.map(ParcelContentTable::toParcelContent));
    }
}
