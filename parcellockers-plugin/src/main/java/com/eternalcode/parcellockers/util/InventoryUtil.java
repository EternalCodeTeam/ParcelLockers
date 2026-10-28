package com.eternalcode.parcellockers.util;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    private InventoryUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
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

    /**
     * Returns whether all of the given items would fit into the player's inventory, accounting for
     * stacking into partially-filled slots rather than only counting fully empty slots.
     */
    public static boolean canHold(Player player, List<ItemStack> items) {
        ItemStack[] contents = player.getInventory().getStorageContents();

        // Simulate placement against a snapshot of the current slot amounts.
        ItemStack[] slotType = new ItemStack[contents.length];
        int[] slotAmount = new int[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                slotType[i] = contents[i];
                slotAmount[i] = contents[i].getAmount();
            }
        }

        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }

            int remaining = item.getAmount();
            int maxStack = item.getMaxStackSize();

            // Top up existing matching stacks first.
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                if (slotType[i] != null && slotType[i].isSimilar(item) && slotAmount[i] < maxStack) {
                    int added = Math.min(maxStack - slotAmount[i], remaining);
                    slotAmount[i] += added;
                    remaining -= added;
                }
            }

            // Then spill into empty slots.
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                if (slotType[i] == null) {
                    slotType[i] = item;
                    int added = Math.min(maxStack, remaining);
                    slotAmount[i] = added;
                    remaining -= added;
                }
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }
}
