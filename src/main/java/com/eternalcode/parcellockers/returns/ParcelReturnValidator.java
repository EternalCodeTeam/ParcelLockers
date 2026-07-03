package com.eternalcode.parcellockers.returns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.bukkit.inventory.ItemStack;

/**
 * Validates that deposited items are, as a multiset, exactly the original parcel content:
 * every deposited stack must be equivalent to some original item, and the total amount per
 * equivalence group must match. Stack splitting or merging is irrelevant.
 */
public class ParcelReturnValidator {

    private final BiPredicate<ItemStack, ItemStack> equivalence;

    public ParcelReturnValidator(BiPredicate<ItemStack, ItemStack> equivalence) {
        this.equivalence = equivalence;
    }

    public boolean matches(List<ItemStack> deposited, List<ItemStack> expected) {
        List<ItemStack> samples = new ArrayList<>();
        List<Integer> expectedTotals = new ArrayList<>();
        List<Integer> depositedTotals = new ArrayList<>();

        for (ItemStack item : expected) {
            int index = this.indexOf(samples, item);
            if (index < 0) {
                samples.add(item);
                expectedTotals.add(item.getAmount());
                depositedTotals.add(0);
                continue;
            }
            expectedTotals.set(index, expectedTotals.get(index) + item.getAmount());
        }

        for (ItemStack item : deposited) {
            int index = this.indexOf(samples, item);
            if (index < 0) {
                return false;
            }
            depositedTotals.set(index, depositedTotals.get(index) + item.getAmount());
        }

        return expectedTotals.equals(depositedTotals);
    }

    private int indexOf(List<ItemStack> samples, ItemStack item) {
        for (int i = 0; i < samples.size(); i++) {
            if (this.equivalence.test(samples.get(i), item)) {
                return i;
            }
        }
        return -1;
    }
}
