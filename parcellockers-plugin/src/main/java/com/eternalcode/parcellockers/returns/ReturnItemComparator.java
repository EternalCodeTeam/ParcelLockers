package com.eternalcode.parcellockers.returns;

import java.util.EnumSet;
import java.util.function.BiPredicate;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ReturnItemComparator extends BiPredicate<ItemStack, ItemStack> {

    EnumSet<ReturnItemDifference> differences(ItemStack expected, ItemStack deposited);

    @Override
    default boolean test(ItemStack expected, ItemStack deposited) {
        return this.differences(expected, deposited).isEmpty();
    }
}
