package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.gui.implementation.locker.LockerMainGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class LockerInteractionController implements Listener {

    private final LockerRepository parcelLockerRepository;
    private final LockerMainGUI lockerMainGUI;

    public LockerInteractionController(LockerRepository parcelLockerRepository, LockerMainGUI lockerMainGUI) {
        this.parcelLockerRepository = parcelLockerRepository;
        this.lockerMainGUI = lockerMainGUI;
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
            this.lockerMainGUI.show(player);
        }
    }
}
