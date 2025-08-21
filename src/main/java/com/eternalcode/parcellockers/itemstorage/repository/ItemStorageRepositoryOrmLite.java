package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemStorageRepositoryOrmLite extends AbstractRepositoryOrmLite implements ItemStorageRepository {

    public ItemStorageRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ItemStorageTable.class);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> save(ItemStorage itemStorage) {
        return this.save(ItemStorageTable.class, ItemStorageTable.from(itemStorage.owner(), itemStorage.items())).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<ItemStorage>> find(UUID uuid) {
        return this.selectSafe(ItemStorageTable.class, uuid).thenApply(optional -> optional.map(ItemStorageTable::toItemStorage));
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
        return this.deleteById(ItemStorageTable.class, uuid);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ItemStorageTable.class);
    }
}
