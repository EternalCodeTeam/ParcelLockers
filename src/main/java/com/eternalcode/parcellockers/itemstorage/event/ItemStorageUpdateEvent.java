package com.eternalcode.parcellockers.itemstorage.event;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ItemStorageUpdateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final ItemStorage oldItemStorage;
    private final ItemStorage updatedItemStorage;

    private boolean cancelled;

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
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
