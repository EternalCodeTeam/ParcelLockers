package com.eternalcode.parcellockers.user.controller;

import com.eternalcode.parcellockers.user.UserService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PrepareUserController implements Listener {

    private final UserService userService;

    public PrepareUserController(UserService userService) {
        this.userService = userService;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.userService.getOrCreate(player.getUniqueId(), player.getName());
    }

}
