package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemStorageRepositoryOrmLite extends AbstractRepositoryOrmLite implements ItemStorageRepository {

    public ItemStorageRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(ItemStorageTable.class);
    }

    @Override
    public CompletableFuture<Void> save(ItemStorage itemStorage) {
        return this.insertIfAbsent(ItemStorageTable.class, ItemStorageTable.from(itemStorage.owner(), itemStorage.items())).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<ItemStorage>> fetch(UUID uuid) {
        return this.selectSafe(ItemStorageTable.class, uuid).thenApply(optional -> optional.map(ItemStorageTable::toItemStorage));
    }

    @Override
    public CompletableFuture<Optional<List<ItemStorage>>> fetchAll() {
        return this.selectAll(ItemStorageTable.class)
            .thenApply(list -> Optional.of(list.stream()
                .map(ItemStorageTable::toItemStorage)
                .toList()));
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
