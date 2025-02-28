package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ItemStorageRepository {

    CompletableFuture<Void> save(ItemStorage itemStorage);

    CompletableFuture<Optional<ItemStorage>> find(UUID uuid);

    CompletableFuture<Integer> remove(UUID uuid);

    CompletableFuture<Integer> removeAll();
}
