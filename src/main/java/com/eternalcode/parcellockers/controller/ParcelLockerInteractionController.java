package com.eternalcode.parcellockers.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.gui.MainGUI;
import com.eternalcode.parcellockers.gui.ParcelLockerMainGUI;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class ParcelLockerInteractionController implements Listener {

    private final ParcelLockerDatabaseService parcelLockerDatabaseService;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;

    public ParcelLockerInteractionController(ParcelLockerDatabaseService parcelLockerDatabaseService, ParcelRepository parcelRepository, MiniMessage miniMessage, Plugin plugin, PluginConfiguration config, MainGUI mainGUI) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
        this.miniMessage = miniMessage;
        this.config = config;
    }

    @EventHandler
    public void onInventoryOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Position blockPos = PositionAdapter.convert(player.getTargetBlock(Set.of(Material.AIR), 5).getLocation());
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST && this.parcelLockerDatabaseService.positionCache().containsKey(blockPos)) {
            event.setCancelled(true);
            new ParcelLockerMainGUI(this.miniMessage, this.config).show(player);
        }
    }
}
