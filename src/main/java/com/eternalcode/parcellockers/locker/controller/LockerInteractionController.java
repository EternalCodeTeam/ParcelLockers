package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.locker.database.LockerDatabaseService;
import com.eternalcode.parcellockers.locker.gui.LockerMainGUI;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class LockerInteractionController implements Listener {

    private final LockerDatabaseService parcelLockerDatabaseService;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;

    public LockerInteractionController(LockerDatabaseService parcelLockerDatabaseService, MiniMessage miniMessage, PluginConfiguration config) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
        this.miniMessage = miniMessage;
        this.config = config;
    }

    @EventHandler
    public void onInventoryOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Position blockPos = PositionAdapter.convert(player.getTargetBlock(Set.of(Material.AIR), 4).getLocation());
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
         }
         
         if (event.getClickedBlock().getType() != Material.CHEST) {
             return;
         }
         
        if (this.parcelLockerDatabaseService.isInCache(blockPos)) {
            event.setCancelled(true);
            new LockerMainGUI(this.miniMessage, this.config).show(player);
        }
    }
}
