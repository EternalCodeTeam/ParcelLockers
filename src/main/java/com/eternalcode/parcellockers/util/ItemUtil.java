package com.eternalcode.parcellockers.util;

import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtil {

    private static final Gson GSON = new Gson();

    private ItemUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean compareMeta(ItemStack first, ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        if (firstMeta == null) {
            return false;
        }
        ItemMeta secondMeta = second.getItemMeta();
        if (secondMeta == null) {
            return false;
        }

        if (first.getType() != second.getType()) {
            return false;
        }
        
        return firstMeta.getLore().containsAll(secondMeta.getLore())
            && firstMeta.getDisplayName().equals(secondMeta.getDisplayName());
    }

    public static String itemStackToString(ItemStack stack) {
        return GSON.toJson(stack);
    }

    public static ItemStack stringToItemStack(String string) {
        return GSON.fromJson(string, ItemStack.class);
    }

}
