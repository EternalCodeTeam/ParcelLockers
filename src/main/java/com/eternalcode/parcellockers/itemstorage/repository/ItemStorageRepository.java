package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ItemStorageRepository {

    CompletableFuture<Void> save(ItemStorage itemStorage);

    CompletableFuture<Optional<ItemStorage>> find(UUID uuid);

    CompletableFuture<Void> remove(UUID uuid);

    CompletableFuture<Void> update(ItemStorage itemStorage);

}
