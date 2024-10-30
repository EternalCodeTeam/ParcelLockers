package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.database.persister.ItemStackPersister;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@DatabaseTable(tableName = "item_storage")
class ItemStorageWrapper {

    @DatabaseField(id = true)
    private UUID uuid;

    @DatabaseField(persisterClass = ItemStackPersister.class)
    private List<ItemStack> items;

    ItemStorageWrapper() {
    }

    ItemStorageWrapper(UUID uuid, List<ItemStack> items) {
        this.uuid = uuid;
        this.items = items;
    }

    public static ItemStorageWrapper from(UUID uuid, List<ItemStack> items) {
        return new ItemStorageWrapper(uuid, items);
    }

    ItemStorage toItemStorage() {
        return new ItemStorage(this.uuid, this.items);
    }

}
