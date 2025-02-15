package com.eternalcode.parcellockers.util;

import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.stream.Collectors;

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

    public static int freeSlotsInInventory(Player player) {
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    public static void shiftItems(int removedSlot, BaseGui gui) {
        Map<Integer, GuiItem> itemsToShift = gui.getGuiItems().entrySet()
            .stream()
            .filter(entry -> entry.getKey() > removedSlot)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int currentShift = removedSlot;
        for (Map.Entry<Integer, GuiItem> entry : itemsToShift.entrySet()) {
            int nextShift = entry.getKey();
            GuiItem item = entry.getValue();
            gui.setItem(currentShift, item);
            currentShift = nextShift;
        }
    }


}
