package com.eternalcode.parcellockers.user.event;

import com.eternalcode.parcellockers.user.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class UserChangeNameEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final User user;
    private final String oldName;

    public UserChangeNameEvent(User user, String oldName) {
        this.user = user;
        this.oldName = oldName;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public User getUser() {
        return this.user;
    }

    public String getOldName() {
        return this.oldName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
