package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.EnumSet;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class ParcelReturnValidatorTest {

    private static final ReturnItemComparator BY_MATERIAL = (expected, deposited) ->
        expected.getType() == deposited.getType()
            ? EnumSet.noneOf(ReturnItemDifference.class)
            : EnumSet.of(ReturnItemDifference.MATERIAL);

    private final ParcelReturnValidator validator = new ParcelReturnValidator(BY_MATERIAL);

    private static ItemStack item(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    private static ItemStack damagedItem(Material type, int amount, int damage) {
        ItemStack item = item(type, amount);
        ItemMeta meta = mock(ItemMeta.class, withSettings().extraInterfaces(Damageable.class));
        when(((Damageable) meta).getDamage()).thenReturn(damage);
        when(item.getItemMeta()).thenReturn(meta);
        return item;
    }

    @Test
    void exactSameStacksMatch() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));

        assertTrue(this.validator.validate(deposited, expected).matches());
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void splitAndMergedStacksStillMatch() {
        assertTrue(this.validator.matches(
            List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34)),
            List.of(item(Material.OAK_LOG, 64))
        ));
        assertTrue(this.validator.matches(
            List.of(item(Material.OAK_LOG, 64)),
            List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34))
        ));
    }

    @Test
    void reportsInsufficientAmountAndUnexpectedMaterialWithValues() {
        ParcelReturnValidationResult result = this.validator.validate(
            List.of(item(Material.DIAMOND, 4), item(Material.DIRT, 2)),
            List.of(item(Material.DIAMOND, 5))
        );

        assertFalse(result.matches());
        assertEquals(List.of(
            ReturnItemMismatch.insufficient(Material.DIAMOND, 5, 4),
            ReturnItemMismatch.unexpected(Material.DIRT, 2)
        ), result.mismatches());
    }

    @Test
    void reportsExcessAmountForExpectedMaterial() {
        ParcelReturnValidationResult result = this.validator.validate(
            List.of(item(Material.DIAMOND, 6)),
            List.of(item(Material.DIAMOND, 5))
        );

        assertEquals(List.of(ReturnItemMismatch.excess(Material.DIAMOND, 5, 6)), result.mismatches());
    }

    @Test
    void emptyDepositReportsEveryMissingMaterialInExpectedOrder() {
        ParcelReturnValidationResult result = this.validator.validate(
            List.of(),
            List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 12))
        );

        assertEquals(List.of(
            ReturnItemMismatch.insufficient(Material.DIAMOND, 5, 0),
            ReturnItemMismatch.insufficient(Material.OAK_LOG, 12, 0)
        ), result.mismatches());
    }

    @Test
    void reportsAllAttributeDifferencesForTheAffectedItem() {
        ItemStack expected = damagedItem(Material.DIAMOND_SWORD, 1, 4);
        ItemStack deposited = damagedItem(Material.DIAMOND_SWORD, 1, 27);
        ReturnItemComparator attributes = (left, right) -> EnumSet.of(
            ReturnItemDifference.DURABILITY,
            ReturnItemDifference.ENCHANTMENTS
        );

        ParcelReturnValidationResult result = new ParcelReturnValidator(attributes)
            .validate(List.of(deposited), List.of(expected));

        assertEquals(List.of(
            ReturnItemMismatch.attribute(ReturnMismatchType.DURABILITY, expected, deposited),
            ReturnItemMismatch.attribute(ReturnMismatchType.ENCHANTMENTS, expected, deposited)
        ), result.mismatches());
    }

    @Test
    void consumesExactVariantBeforeDiagnosingClosestVariant() {
        ItemStack expectedNamed = item(Material.DIAMOND_SWORD, 1);
        ItemStack expectedEnchanted = item(Material.DIAMOND_SWORD, 1);
        ItemStack depositedExact = item(Material.DIAMOND_SWORD, 1);
        ItemStack depositedChangedName = item(Material.DIAMOND_SWORD, 1);

        ReturnItemComparator variants = (expected, deposited) -> {
            if (expected == expectedEnchanted && deposited == depositedExact) {
                return EnumSet.noneOf(ReturnItemDifference.class);
            }
            if (expected == expectedNamed && deposited == depositedChangedName) {
                return EnumSet.of(ReturnItemDifference.ITEM_NAME);
            }
            return EnumSet.of(ReturnItemDifference.ITEM_NAME, ReturnItemDifference.ENCHANTMENTS);
        };

        ParcelReturnValidationResult result = new ParcelReturnValidator(variants).validate(
            List.of(depositedExact, depositedChangedName),
            List.of(expectedNamed, expectedEnchanted)
        );

        assertEquals(List.of(
            ReturnItemMismatch.attribute(ReturnMismatchType.ITEM_NAME, expectedNamed, depositedChangedName)
        ), result.mismatches());
    }

    @Test
    void choosesThePairWithTheFewestDifferences() {
        ItemStack expectedNamed = item(Material.DIAMOND_SWORD, 1);
        ItemStack expectedEnchanted = item(Material.DIAMOND_SWORD, 1);
        ItemStack depositedEnchanted = item(Material.DIAMOND_SWORD, 1);
        ItemStack depositedNamed = item(Material.DIAMOND_SWORD, 1);

        ReturnItemComparator variants = (expected, deposited) -> {
            if (expected == expectedNamed && deposited == depositedNamed) {
                return EnumSet.of(ReturnItemDifference.ITEM_NAME);
            }
            if (expected == expectedEnchanted && deposited == depositedEnchanted) {
                return EnumSet.of(ReturnItemDifference.ENCHANTMENTS);
            }
            return EnumSet.of(ReturnItemDifference.ITEM_NAME, ReturnItemDifference.ENCHANTMENTS);
        };

        ParcelReturnValidationResult result = new ParcelReturnValidator(variants).validate(
            List.of(depositedEnchanted, depositedNamed),
            List.of(expectedNamed, expectedEnchanted)
        );

        assertEquals(List.of(
            ReturnItemMismatch.attribute(ReturnMismatchType.ITEM_NAME, expectedNamed, depositedNamed),
            ReturnItemMismatch.attribute(ReturnMismatchType.ENCHANTMENTS, expectedEnchanted, depositedEnchanted)
        ), result.mismatches());
    }

    @Test
    void aggregatesDuplicateAttributeReasons() {
        ItemStack firstExpected = item(Material.DIAMOND_SWORD, 1);
        ItemStack secondExpected = item(Material.DIAMOND_SWORD, 1);
        ItemStack firstDeposited = item(Material.DIAMOND_SWORD, 1);
        ItemStack secondDeposited = item(Material.DIAMOND_SWORD, 1);
        ReturnItemComparator enchantments = (expected, deposited) ->
            EnumSet.of(ReturnItemDifference.ENCHANTMENTS);

        ParcelReturnValidationResult result = new ParcelReturnValidator(enchantments).validate(
            List.of(firstDeposited, secondDeposited),
            List.of(firstExpected, secondExpected)
        );

        assertEquals(List.of(
            ReturnItemMismatch.attribute(ReturnMismatchType.ENCHANTMENTS, firstExpected, firstDeposited)
        ), result.mismatches());
    }

    @Test
    void resolvesEqualDifferenceCountsInInputOrder() {
        ItemStack firstExpected = damagedItem(Material.DIAMOND_SWORD, 1, 1);
        ItemStack secondExpected = damagedItem(Material.DIAMOND_SWORD, 1, 2);
        ItemStack firstDeposited = damagedItem(Material.DIAMOND_SWORD, 1, 10);
        ItemStack secondDeposited = damagedItem(Material.DIAMOND_SWORD, 1, 20);
        ReturnItemComparator durability = (expected, deposited) ->
            EnumSet.of(ReturnItemDifference.DURABILITY);

        ParcelReturnValidationResult result = new ParcelReturnValidator(durability).validate(
            List.of(firstDeposited, secondDeposited),
            List.of(firstExpected, secondExpected)
        );

        assertEquals(List.of(
            ReturnItemMismatch.attribute(ReturnMismatchType.DURABILITY, firstExpected, firstDeposited),
            ReturnItemMismatch.attribute(ReturnMismatchType.DURABILITY, secondExpected, secondDeposited)
        ), result.mismatches());
    }
}
