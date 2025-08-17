package com.eternalcode.parcellockers.user.controller;

import com.eternalcode.parcellockers.user.UserService;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class LoadUserController implements Listener {

    private final UserService userService;
    private final Server server;

    public LoadUserController(UserService userService, Server server) {
        this.userService = userService;
        this.server = server;
    }

    @EventHandler
    void onLoad(ServerLoadEvent event) {
        for (Player player : this.server.getOnlinePlayers()) {
            this.userService.create(player.getUniqueId(), player.getName());
        }
    }
}
