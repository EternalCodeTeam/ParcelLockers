package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.exception.DatabaseException;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelContentRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelContentRepository {

    public ParcelContentRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelContentTable.class);
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to create ParcelContent table", exception);
        }
    }

    @Override
    public CompletableFuture<Void> save(ParcelContent parcelContent) {
        return this.saveIfNotExist(ParcelContentTable.class, ParcelContentTable.from(parcelContent)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uniqueId) {
        return this.deleteById(ParcelContentTable.class, uniqueId);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ParcelContentTable.class);
    }

    @Override
    public CompletableFuture<Optional<ParcelContent>> fetch(UUID uniqueId) {
        return this.selectSafe(ParcelContentTable.class, uniqueId)
            .thenApply(optional -> optional.map(ParcelContentTable::toParcelContent));
    }
}
