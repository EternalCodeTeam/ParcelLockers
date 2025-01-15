package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.j256.ormlite.table.TableUtils;
import io.sentry.Sentry;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelContentRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelContentRepository {

    public ParcelContentRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelContentWrapper.class);
        } catch (SQLException exception) {
            Sentry.captureException(exception);
            throw new RuntimeException("Failed to create ParcelContent table", exception);
        }
    }

    @Override
    public CompletableFuture<Void> save(ParcelContent parcelContent) {
        return this.saveIfNotExist(ParcelContentWrapper.class, ParcelContentWrapper.from(parcelContent)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uniqueId) {
        return this.deleteById(ParcelContentWrapper.class, uniqueId);
    }

    @Override
    public CompletableFuture<Optional<ParcelContent>> find(UUID uniqueId) {
        return this.select(ParcelContentWrapper.class, uniqueId).thenApply(parcelContentWrapper -> Optional.ofNullable(parcelContentWrapper.toParcelContent()));
    }
}
