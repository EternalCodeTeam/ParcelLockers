package com.eternalcode.parcellockers.util;

import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {

    private static final Gson GSON = new Gson();

    private ItemUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean compareMeta(ItemStack first, ItemStack second) {
        if (!first.hasItemMeta()) {
            return false;
        }

        if (second.hasItemMeta()) {
            return false;
        }

        return first.getType() == second.getType()
            && first.getItemMeta().getLore().containsAll(second.getItemMeta().getLore())
            && first.getItemMeta().getDisplayName().equals(second.getItemMeta().getDisplayName());
    }

    public static String itemStackToString(ItemStack stack) {
        return GSON.toJson(stack);
    }

    public static ItemStack stringToItemStack(String string) {
        return GSON.fromJson(string, ItemStack.class);
    }

}
