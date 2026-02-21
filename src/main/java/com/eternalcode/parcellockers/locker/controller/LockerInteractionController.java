package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.gui.implementation.locker.LockerGui;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.nexo.NexoIntegration;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LockerInteractionController implements Listener {

    private final LockerManager lockerManager;
    private final LockerGui lockerGUI;
    private final Scheduler scheduler;

    public LockerInteractionController(LockerManager lockerManager, LockerGui lockerGUI, Scheduler scheduler) {
        this.lockerManager = lockerManager;
        this.lockerGUI = lockerGUI;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onInventoryOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block.getType() != Material.CHEST && !NexoIntegration.isNexoBlock(block)) {
            return;
        }

        this.lockerManager.get(PositionAdapter.convert(block.getLocation())).thenAccept(optionalLocker -> {
            if (optionalLocker.isEmpty()) {
                return;
            }
            UUID uuid = optionalLocker.get().uuid();

            this.scheduler.run(() -> {
                event.setCancelled(true);
                this.lockerGUI.show(player, uuid);
            });
        });
    }
}
