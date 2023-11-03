package com.eternalcode.parcellockers.feature.itemstorage.repository;

import com.eternalcode.parcellockers.feature.itemstorage.ItemStorage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ItemStorageRepository {

    CompletableFuture<Void> save(ItemStorage itemStorage);

    CompletableFuture<Optional<ItemStorage>> find(UUID uuid);

}
