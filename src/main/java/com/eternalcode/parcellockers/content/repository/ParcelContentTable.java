package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.persister.ItemStackPersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

@DatabaseTable(tableName = "parcel_content")
class ParcelContentTable {

    @DatabaseField(id = true)
    private UUID uniqueId;

    @DatabaseField(persisterClass = ItemStackPersister.class)
    private List<ItemStack> content;

    ParcelContentTable() {
    }

    ParcelContentTable(UUID uniqueId, List<ItemStack> content) {
        this.uniqueId = uniqueId;
        this.content = content;
    }

    static ParcelContentTable from(ParcelContent parcelContent) {
        return new ParcelContentTable(parcelContent.uniqueId(), parcelContent.items());
    }

    ParcelContent toParcelContent() {
        return new ParcelContent(this.uniqueId, this.content);
    }
}
