package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.gui.implementation.locker.LockerMainGui;
import com.eternalcode.parcellockers.locker.repository.LockerCache;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LockerInteractionController implements Listener {

    private final LockerCache cache;
    private final LockerMainGui lockerMainGUI;

    public LockerInteractionController(LockerCache cache, LockerMainGui lockerMainGUI) {
        this.cache = cache;
        this.lockerMainGUI = lockerMainGUI;
    }

    @EventHandler
    public void onInventoryOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block.getType() != Material.CHEST) {
            return;
        }

        Position blockPos = PositionAdapter.convert(block.getLocation());

        if (this.cache.get(blockPos).isPresent()) {
            event.setCancelled(true);
            this.lockerMainGUI.show(player);
        }
    }
}
