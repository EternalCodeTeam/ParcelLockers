package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.implementation.locker.LockerMainGUI;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
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

public class LockerInteractionController implements Listener {

    private final Plugin plugin;
    private final ParcelRepository parcelRepository;
    private final LockerRepository parcelLockerRepository;
    private final ItemStorageRepository itemStorageRepository;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;

    public LockerInteractionController(Plugin plugin, ParcelRepository parcelRepository, LockerRepository parcelLockerRepository, ItemStorageRepository itemStorageRepository, MiniMessage miniMessage, PluginConfiguration config, NotificationAnnouncer announcer) {
        this.plugin = plugin;
        this.parcelRepository = parcelRepository;
        this.parcelLockerRepository = parcelLockerRepository;
        this.itemStorageRepository = itemStorageRepository;
        this.miniMessage = miniMessage;
        this.config = config;
        this.announcer = announcer;
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

        if (this.parcelLockerRepository.isInCache(blockPos)) {
            event.setCancelled(true);
            new LockerMainGUI(plugin, this.miniMessage, this.config, itemStorageRepository, parcelRepository, announcer).show(player);
        }
    }
}
