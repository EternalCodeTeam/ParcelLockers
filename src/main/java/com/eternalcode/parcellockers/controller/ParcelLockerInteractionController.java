package com.eternalcode.parcellockers.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.gui.ParcelLockerMainGUI;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class ParcelLockerInteractionController implements Listener {

    private final ParcelLockerDatabaseService parcelLockerDatabaseService;
    private final ParcelRepository parcelRepository;
    private final MiniMessage miniMessage;
    private final Plugin plugin;
    private final PluginConfiguration config;

    public ParcelLockerInteractionController(ParcelLockerDatabaseService parcelLockerDatabaseService, ParcelRepository parcelRepository, MiniMessage miniMessage, Plugin plugin, PluginConfiguration config) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
        this.parcelRepository = parcelRepository;
        this.miniMessage = miniMessage;
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        Position blockPos = PositionAdapter.convert(player.getTargetBlock(Set.of(Material.AIR), 5).getLocation());
        if (inventory.getType() == InventoryType.CHEST && this.parcelLockerDatabaseService.cache()
            .values()
            .stream()
            .anyMatch(parcelLocker -> parcelLocker.position().equals(blockPos))) {
                event.setCancelled(true);
                new ParcelLockerMainGUI(this.miniMessage, this.plugin, this.parcelRepository, this.parcelLockerDatabaseService, this.config).show(player);
        }
    }
}
