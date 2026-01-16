package com.eternalcode.parcellockers.locker.event;

import com.eternalcode.parcellockers.locker.Locker;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LockerCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Locker locker;
    private final UUID player;
    private boolean cancelled;

    public LockerCreateEvent(Locker locker, UUID player) {
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
