package com.eternalcode.parcellockers.listener;

import com.eternalcode.parcellockers.database.LastLoginStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;

public class PlayerQuitListener implements Listener {

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LastLoginStorage.lastLoginMap.put(event.getPlayer().getUniqueId(), Instant.now());
    }
}
