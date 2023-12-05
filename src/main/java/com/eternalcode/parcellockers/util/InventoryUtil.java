package com.eternalcode.parcellockers.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static void addItem(Player player, ItemStack itemStack) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null) {
                inventory.addItem(itemStack);
                return;
            }
            player.getWorld().dropItem(player.getLocation(), itemStack);
        }
    }
}
