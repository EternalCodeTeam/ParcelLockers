package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiPredicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ParcelReturnValidatorTest {

    /** Equivalence by material only — the real attribute logic is tested in ReturnItemEquivalenceTest. */
    private static final BiPredicate<ItemStack, ItemStack> BY_MATERIAL = (a, b) -> a.getType() == b.getType();

    private final ParcelReturnValidator validator = new ParcelReturnValidator(BY_MATERIAL);

    private static ItemStack item(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    @Test
    void exactSameStacksMatch() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 5), item(Material.OAK_LOG, 64));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void splitStacksStillMatch() {
        List<ItemStack> expected = List.of(item(Material.OAK_LOG, 64));
        List<ItemStack> deposited = List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void mergedStacksStillMatch() {
        List<ItemStack> expected = List.of(item(Material.OAK_LOG, 30), item(Material.OAK_LOG, 34));
        List<ItemStack> deposited = List.of(item(Material.OAK_LOG, 64));
        assertTrue(this.validator.matches(deposited, expected));
    }

    @Test
    void missingAmountFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 4));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void extraAmountFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 6));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void wrongTypeFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.EMERALD, 5));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void extraForeignItemFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        List<ItemStack> deposited = List.of(item(Material.DIAMOND, 5), item(Material.DIRT, 1));
        assertFalse(this.validator.matches(deposited, expected));
    }

    @Test
    void emptyDepositAgainstNonEmptyContentFails() {
        List<ItemStack> expected = List.of(item(Material.DIAMOND, 5));
        assertFalse(this.validator.matches(List.of(), expected));
    }
}
