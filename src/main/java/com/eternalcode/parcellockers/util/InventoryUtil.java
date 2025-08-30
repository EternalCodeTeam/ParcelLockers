package com.eternalcode.parcellockers.util;

import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
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

    public static void shiftItems(int removedSlot, PaginatedGui gui, Material typeToShift) {
        if (removedSlot < 0 || gui == null) {
            throw new IllegalArgumentException("Invalid arguments provided for shifting items.");
        }
        // Pobierz wszystkie itemy do przesunięcia (tylko te po usuniętym slocie)
        List<Entry<Integer, GuiItem>> itemsToShift = gui.getGuiItems().entrySet()
            .stream()
            .filter(entry -> entry.getKey() > removedSlot)
            .filter(entry -> entry.getValue().getItemStack().getType() == typeToShift)
            .sorted(Comparator.comparingInt(Map.Entry::getKey)) // Sortowanie rosnące według slotu
            .toList();

        // Jeśli nie ma itemów do przesunięcia, zakończ
        if (itemsToShift.isEmpty()) {
            return;
        }

        // Usuń itemy z ich obecnych pozycji (od końca, żeby nie pomieszać indeksów)
        for (int i = itemsToShift.size() - 1; i >= 0; i--) {
            gui.removeItem(itemsToShift.get(i).getKey());
        }

        // Przesuń itemy w lewo - każdy item trafia do slotu o 1 mniejszego niż był
        int targetSlot = removedSlot;
        for (Map.Entry<Integer, GuiItem> entry : itemsToShift) {
            gui.setItem(targetSlot, entry.getValue());
            targetSlot++;
        }
    }
}
