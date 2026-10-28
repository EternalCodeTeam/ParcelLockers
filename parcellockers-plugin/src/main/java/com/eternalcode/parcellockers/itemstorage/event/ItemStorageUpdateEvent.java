package com.eternalcode.parcellockers.itemstorage.event;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.shared.event.CancellableEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ItemStorageUpdateEvent extends CancellableEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final ItemStorage oldItemStorage;
    private final ItemStorage updatedItemStorage;

    public ItemStorageUpdateEvent(ItemStorage oldItemStorage, ItemStorage updatedItemStorage) {
        super(true);
        this.oldItemStorage = oldItemStorage;
        this.updatedItemStorage = updatedItemStorage;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public ItemStorage getOldItemStorage() {
        return this.oldItemStorage;
    }

    public ItemStorage getUpdatedItemStorage() {
        return this.updatedItemStorage;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
