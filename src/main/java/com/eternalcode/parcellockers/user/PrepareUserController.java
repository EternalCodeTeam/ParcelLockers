package com.eternalcode.parcellockers.user;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PrepareUserController implements Listener {

    private final UserManager userManager;

    public PrepareUserController(UserManager userManager) {
        this.userManager = userManager;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.userManager.getOrCreate(player.getUniqueId(), player.getName());
    }

}
