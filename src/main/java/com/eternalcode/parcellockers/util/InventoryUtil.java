package com.eternalcode.parcellockers.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static void addItem(Player player, ItemStack itemStack) {
        Inventory inv = player.getInventory();
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null) {
                inv.addItem(itemStack);
                return;
            }
            player.getWorld().dropItem(player.getLocation(), itemStack);
        }
    }
}
