package com.eternalcode.parcellockers.user.event;

import com.eternalcode.parcellockers.user.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class UserCreateEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final User user;

    public UserCreateEvent(User user) {
        this.user = user;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public User getUser() {
        return this.user;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
