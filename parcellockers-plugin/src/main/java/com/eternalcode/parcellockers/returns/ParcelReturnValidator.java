package com.eternalcode.parcellockers.returns;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Validates deposited items as a multiset. Physical stack splitting and merging do not
 * affect validation. Exact variants are consumed before altered variants are diagnosed.
 */
public class ParcelReturnValidator {

    private final ReturnItemComparator comparator;

    public ParcelReturnValidator(ReturnItemComparator comparator) {
        this.comparator = comparator;
    }

    public ParcelReturnValidationResult validate(List<ItemStack> deposited, List<ItemStack> expected) {
        Objects.requireNonNull(deposited, "Deposited items cannot be null");
        Objects.requireNonNull(expected, "Expected items cannot be null");

        Set<ReturnItemMismatch> mismatches = new LinkedHashSet<>();
        Map<Material, Integer> expectedTotals = totals(expected);
        Map<Material, Integer> depositedTotals = totals(deposited);

        for (Map.Entry<Material, Integer> entry : expectedTotals.entrySet()) {
            int expectedAmount = entry.getValue();
            int depositedAmount = depositedTotals.getOrDefault(entry.getKey(), 0);
            if (depositedAmount < expectedAmount) {
                mismatches.add(ReturnItemMismatch.insufficient(entry.getKey(), expectedAmount, depositedAmount));
            } else if (depositedAmount > expectedAmount) {
                mismatches.add(ReturnItemMismatch.excess(entry.getKey(), expectedAmount, depositedAmount));
            }
        }
        for (Map.Entry<Material, Integer> entry : depositedTotals.entrySet()) {
            if (!expectedTotals.containsKey(entry.getKey())) {
                mismatches.add(ReturnItemMismatch.unexpected(entry.getKey(), entry.getValue()));
            }
        }

        List<Candidate> expectedCandidates = candidates(expected);
        List<Candidate> depositedCandidates = candidates(deposited);
        this.consumeExact(expectedCandidates, depositedCandidates);
        this.diagnoseClosest(expectedCandidates, depositedCandidates, mismatches);

        return new ParcelReturnValidationResult(List.copyOf(mismatches));
    }

    public boolean matches(List<ItemStack> deposited, List<ItemStack> expected) {
        return this.validate(deposited, expected).matches();
    }

    private void consumeExact(List<Candidate> expected, List<Candidate> deposited) {
        for (Candidate expectedItem : expected) {
            for (Candidate depositedItem : deposited) {
                if (expectedItem.remaining == 0 || depositedItem.remaining == 0) {
                    continue;
                }
                if (this.comparator.test(expectedItem.item, depositedItem.item)) {
                    consume(expectedItem, depositedItem);
                }
            }
        }
    }

    private void diagnoseClosest(
        List<Candidate> expected,
        List<Candidate> deposited,
        Set<ReturnItemMismatch> mismatches
    ) {
        while (true) {
            Pair closest = this.closestPair(expected, deposited);
            if (closest == null) {
                return;
            }
            Candidate expectedItem = expected.get(closest.expectedIndex);
            Candidate depositedItem = deposited.get(closest.depositedIndex);
            for (ReturnItemDifference difference : closest.differences) {
                mismatches.add(ReturnItemMismatch.attribute(
                    mismatchType(difference),
                    expectedItem.item,
                    depositedItem.item
                ));
            }
            consume(expectedItem, depositedItem);
        }
    }

    private Pair closestPair(List<Candidate> expected, List<Candidate> deposited) {
        Pair closest = null;
        for (int expectedIndex = 0; expectedIndex < expected.size(); expectedIndex++) {
            Candidate expectedItem = expected.get(expectedIndex);
            if (expectedItem.remaining == 0) {
                continue;
            }
            for (int depositedIndex = 0; depositedIndex < deposited.size(); depositedIndex++) {
                Candidate depositedItem = deposited.get(depositedIndex);
                if (depositedItem.remaining == 0 || expectedItem.item.getType() != depositedItem.item.getType()) {
                    continue;
                }
                EnumSet<ReturnItemDifference> differences = this.comparator.differences(
                    expectedItem.item,
                    depositedItem.item
                );
                if (differences.isEmpty() || differences.contains(ReturnItemDifference.MATERIAL)) {
                    continue;
                }
                if (closest == null || differences.size() < closest.differences.size()) {
                    closest = new Pair(expectedIndex, depositedIndex, EnumSet.copyOf(differences));
                }
            }
        }
        return closest;
    }

    private static ReturnMismatchType mismatchType(ReturnItemDifference difference) {
        return switch (difference) {
            case DURABILITY -> ReturnMismatchType.DURABILITY;
            case ITEM_NAME -> ReturnMismatchType.ITEM_NAME;
            case ENCHANTMENTS -> ReturnMismatchType.ENCHANTMENTS;
            case LORE -> ReturnMismatchType.LORE;
            case NBT -> ReturnMismatchType.NBT;
            case MATERIAL -> throw new IllegalArgumentException("Material differences are not attribute mismatches");
        };
    }

    private static Map<Material, Integer> totals(List<ItemStack> items) {
        Map<Material, Integer> totals = new LinkedHashMap<>();
        for (ItemStack item : items) {
            totals.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return totals;
    }

    private static List<Candidate> candidates(List<ItemStack> items) {
        List<Candidate> candidates = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            candidates.add(new Candidate(item));
        }
        return candidates;
    }

    private static void consume(Candidate expected, Candidate deposited) {
        int amount = Math.min(expected.remaining, deposited.remaining);
        expected.remaining -= amount;
        deposited.remaining -= amount;
    }

    private static final class Candidate {
        private final ItemStack item;
        private int remaining;

        private Candidate(ItemStack item) {
            this.item = item;
            this.remaining = item.getAmount();
        }
    }

    private record Pair(
        int expectedIndex,
        int depositedIndex,
        EnumSet<ReturnItemDifference> differences
    ) {
    }
}
