package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.persister.ItemStackPersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@DatabaseTable(tableName = "parcel_content")
public class ParcelContentWrapper {

    @DatabaseField(id = true)
    private UUID uniqueId;

    @DatabaseField(persisterClass = ItemStackPersister.class)
    private List<ItemStack> content;

    ParcelContentWrapper() {
    }

    ParcelContentWrapper(UUID uniqueId, List<ItemStack> content) {
        this.uniqueId = uniqueId;
        this.content = content;
    }

    static ParcelContentWrapper from(ParcelContent parcelContent) {
        return new ParcelContentWrapper(parcelContent.uniqueId(), parcelContent.items());
    }

    ParcelContent toParcelContent() {
        return new ParcelContent(this.uniqueId, this.content);
    }
}
