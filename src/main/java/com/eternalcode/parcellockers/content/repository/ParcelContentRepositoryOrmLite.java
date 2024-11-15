package com.eternalcode.parcellockers.content.repository;

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

    public ParcelContentRepositoryOrmLite(DatabaseManager databaseManager) {
        super(databaseManager);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelContentWrapper.class);
        } catch (SQLException exception) {
            Sentry.captureException(exception);
            exception.printStackTrace();
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
    public CompletableFuture<Void> update(ParcelContent parcelContent) {
        return this.save(ParcelContentWrapper.class, ParcelContentWrapper.from(parcelContent)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<ParcelContent>> find(UUID uniqueId) {
        return this.select(ParcelContentWrapper.class, uniqueId).thenApply(parcelContentWrapper -> Optional.ofNullable(parcelContentWrapper.toParcelContent()));
    }
}
