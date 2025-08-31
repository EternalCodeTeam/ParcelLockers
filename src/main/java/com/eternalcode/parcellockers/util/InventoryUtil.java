package com.eternalcode.parcellockers.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static int freeSlotsInInventory(Player player) {
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) {
                freeSlots++;
            }
        }
        return freeSlots;
    }
}
