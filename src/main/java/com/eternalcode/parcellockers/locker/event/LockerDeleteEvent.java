package com.eternalcode.parcellockers.locker.event;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.event.CancellableEvent;
import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

// Called when a locker is deleted. Fired synchronously on the main thread (see LockerManager#delete).
// Warning: this event is not called when all lockers are deleted through "/parcel debug delete lockers" command
public class LockerDeleteEvent extends CancellableEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Locker locker;
    private final UUID player;

    public LockerDeleteEvent(Locker locker, UUID player) {
        this.locker = locker;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Locker getLocker() {
        return this.locker;
    }

    public UUID getPlayer() {
        return this.player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
