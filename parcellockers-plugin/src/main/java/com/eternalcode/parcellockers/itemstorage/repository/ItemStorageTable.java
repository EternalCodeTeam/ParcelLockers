package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.database.persister.ItemStackPersister;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

@DatabaseTable(tableName = "item_storage")
class ItemStorageTable {

    @DatabaseField(id = true)
    private UUID uuid;

    @DatabaseField(persisterClass = ItemStackPersister.class)
    private List<ItemStack> items;

    ItemStorageTable() {
    }

    ItemStorageTable(UUID uuid, List<ItemStack> items) {
        this.uuid = uuid;
        this.items = items;
    }

    public static ItemStorageTable from(UUID uuid, List<ItemStack> items) {
        return new ItemStorageTable(uuid, items);
    }

    ItemStorage toItemStorage() {
        return new ItemStorage(this.uuid, this.items);
    }

}
