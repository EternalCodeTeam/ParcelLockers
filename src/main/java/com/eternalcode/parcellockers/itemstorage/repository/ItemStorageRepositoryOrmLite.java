package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.j256.ormlite.table.TableUtils;
import io.sentry.Sentry;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemStorageRepositoryOrmLite extends AbstractRepositoryOrmLite implements ItemStorageRepository {

    public ItemStorageRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ItemStorageWrapper.class);
        } catch (SQLException ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> save(ItemStorage itemStorage) {
        return this.save(ItemStorageWrapper.class, ItemStorageWrapper.from(itemStorage.owner(), itemStorage.items())).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<ItemStorage>> find(UUID uuid) {
        return this.selectSafe(ItemStorageWrapper.class, uuid).thenApply(optional -> optional.map(ItemStorageWrapper::toItemStorage));
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uuid) {
        return this.deleteById(ItemStorageWrapper.class, uuid);
    }
}
