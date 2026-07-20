package com.eternalcode.parcellockers.shared.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * Base class for the plugin's cancellable Bukkit events. Provides the cancel flag and the
 * {@link Cancellable} implementation; subclasses still declare their own {@code HandlerList}
 * (and static {@code getHandlerList()}) as Bukkit requires one per event type.
 */
public abstract class CancellableEvent extends Event implements Cancellable {

    private boolean cancelled;

    protected CancellableEvent() {
    }

    protected CancellableEvent(boolean async) {
        super(async);
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
