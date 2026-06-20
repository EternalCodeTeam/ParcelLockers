package com.eternalcode.parcellockers.parcel.event;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.event.CancellableEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ParcelCollectEvent extends CancellableEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Parcel parcel;

    public ParcelCollectEvent(Parcel parcel) {
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
}
