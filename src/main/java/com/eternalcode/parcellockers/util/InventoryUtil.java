package com.eternalcode.parcellockers.util;

import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.stream.Collectors;

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

    public static void shiftItems(int removedSlot, BaseGui gui, Material typeToShift) {
        if (removedSlot < 0 || gui == null) {
            return;
        }

        Map<Integer, GuiItem> itemsToShift = gui.getGuiItems().entrySet()
            .stream()
            .filter(entry -> entry.getKey() > removedSlot)
            .filter(entry -> entry.getValue().getItemStack().getType() == typeToShift)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<Integer, GuiItem> entry : itemsToShift.entrySet()) {
            GuiItem item = entry.getValue();
            gui.setItem(entry.getKey() - 1, item);
        }
    }
}
