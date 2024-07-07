package com.eternalcode.parcellockers.user;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class LoadUserController implements Listener {

    private final UserManager userManager;
    private final Server server;

    public LoadUserController(UserManager userManager, Server server) {
        this.userManager = userManager;
        this.server = server;
    }

    @EventHandler
    void onLoad(ServerLoadEvent event) {
        for (Player player : this.server.getOnlinePlayers()) {
            this.userManager.create(player.getUniqueId(), player.getName());
        }
    }
}
