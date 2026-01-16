package com.eternalcode.parcellockers.parcel.event;

import com.eternalcode.parcellockers.parcel.Parcel;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ParcelCollectEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Parcel parcel;
    private boolean cancelled;

    public ParcelCollectEvent(Parcel parcel) {
        super(true);
        this.parcel = parcel;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Parcel getParcel() {
        return this.parcel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
