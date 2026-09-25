package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ItemStorageRepository {

    CompletableFuture<Void> save(ItemStorage itemStorage);

    CompletableFuture<Optional<ItemStorage>> fetch(UUID uuid);

    CompletableFuture<Optional<List<ItemStorage>>> fetchAll();

    CompletableFuture<Integer> delete(UUID uuid);

    CompletableFuture<Integer> deleteAll();
}
