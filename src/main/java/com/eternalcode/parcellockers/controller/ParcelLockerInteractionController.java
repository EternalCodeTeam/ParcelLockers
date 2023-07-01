package com.eternalcode.parcellockers.controller;

import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.Set;

public class ParcelLockerInteractionController implements Listener {

    private final ParcelLockerDatabaseService parcelLockerDatabaseService;

    public ParcelLockerInteractionController(ParcelLockerDatabaseService parcelLockerDatabaseService) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Position blockPos = PositionAdapter.convert(event.getPlayer().getTargetBlock(Set.of(Material.AIR), 5).getLocation());
        if (inventory.getType() == InventoryType.CHEST && this.parcelLockerDatabaseService.cache()
            .values()
            .stream()
            .anyMatch(parcelLocker -> parcelLocker.position().equals(blockPos))) {
                event.setCancelled(true);
                // TODO: Open parcel locker inventory
        }
    }
}
