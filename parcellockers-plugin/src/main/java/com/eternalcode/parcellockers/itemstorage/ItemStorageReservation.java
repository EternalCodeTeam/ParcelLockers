package com.eternalcode.parcellockers.itemstorage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.inventory.ItemStack;

public interface ItemStorageReservation extends AutoCloseable {

    UUID owner();

    CompletableFuture<Boolean> delete();

    CompletableFuture<ItemStorage> restore(List<ItemStack> items);

    @Override
    void close();
}
