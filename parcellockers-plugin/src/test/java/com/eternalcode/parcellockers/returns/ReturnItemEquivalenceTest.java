package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class ReturnItemEquivalenceTest {

    private static PluginConfig.ReturnChecks checks(boolean durability, boolean name, boolean enchants, boolean lore, boolean nbt) {
        PluginConfig.ReturnChecks checks = new PluginConfig.ReturnChecks();
        checks.checkDurability = durability;
        checks.checkItemName = name;
        checks.checkEnchantments = enchants;
        checks.checkLore = lore;
        checks.checkNbt = nbt;
        return checks;
    }

    private static ItemStack item(Material type) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        return item;
    }

    private static ItemMeta damagedMeta(int damage) {
        ItemMeta meta = mock(ItemMeta.class, withSettings().extraInterfaces(Damageable.class));
        when(((Damageable) meta).getDamage()).thenReturn(damage);
        when(meta.getEnchants()).thenReturn(Map.of());
        return meta;
    }

    @Test
    void reportsEveryEnabledExplicitDifferenceWithoutDuplicatingNbt() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);
        ItemMeta expectedMeta = damagedMeta(4);
        ItemMeta actualMeta = damagedMeta(27);
        when(expectedMeta.getEnchants()).thenReturn(enchantsOf("sharpness", 3));
        when(actualMeta.getEnchants()).thenReturn(Map.of());
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        ItemStack normalizedExpected = normalizedClone(expected);
        ItemStack normalizedActual = normalizedClone(actual);
        when(normalizedExpected.isSimilar(normalizedActual)).thenReturn(true);

        ReturnItemEquivalence equivalence = new ReturnItemEquivalence(checks(true, true, true, true, true));

        assertEquals(
            EnumSet.of(ReturnItemDifference.DURABILITY, ReturnItemDifference.ENCHANTMENTS),
            equivalence.differences(expected, actual)
        );
        assertFalse(equivalence.test(expected, actual));
    }

    @Test
    void reportsResidualNbtOnlyWhenEnabled() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);
        ItemMeta expectedMeta = damagedMeta(0);
        ItemMeta actualMeta = damagedMeta(0);
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        ItemStack normalizedExpected = normalizedClone(expected);
        ItemStack normalizedActual = normalizedClone(actual);
        when(normalizedExpected.isSimilar(normalizedActual)).thenReturn(false);

        assertEquals(
            EnumSet.of(ReturnItemDifference.NBT),
            new ReturnItemEquivalence(checks(false, false, false, false, true)).differences(expected, actual)
        );
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    private static ItemStack normalizedClone(ItemStack original) {
        ItemStack clone = item(original.getType());
        ItemMeta meta = damagedMeta(0);
        when(clone.getItemMeta()).thenReturn(meta);
        when(original.clone()).thenReturn(clone);
        return clone;
    }

    @Test
    void differentMaterialNeverMatches() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.IRON_SWORD);

        ReturnItemEquivalence equivalence = new ReturnItemEquivalence(checks(false, false, false, false, false));
        assertFalse(equivalence.test(expected, actual));
    }

    @Test
    void durabilityMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);
        ItemMeta expectedMeta = damagedMeta(0);
        ItemMeta actualMeta = damagedMeta(100);
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        assertFalse(new ReturnItemEquivalence(checks(true, false, false, false, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @Test
    void enchantmentMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);

        ItemMeta expectedMeta = mock(ItemMeta.class);
        ItemMeta actualMeta = mock(ItemMeta.class);
        when(expectedMeta.getEnchants()).thenReturn(enchantsOf("sharpness", 3));
        when(actualMeta.getEnchants()).thenReturn(Map.of());
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        assertFalse(new ReturnItemEquivalence(checks(false, false, true, false, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<Enchantment, Integer> enchantsOf(String key, int level) {
        return (Map) Map.of(key, level);
    }

    @Test
    void nameMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);

        ItemMeta expectedMeta = mock(ItemMeta.class);
        ItemMeta actualMeta = mock(ItemMeta.class);
        when(expectedMeta.displayName()).thenReturn(Component.text("Old Sword"));
        when(actualMeta.displayName()).thenReturn(Component.text("New Sword"));
        when(expectedMeta.getEnchants()).thenReturn(Map.of());
        when(actualMeta.getEnchants()).thenReturn(Map.of());
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        assertFalse(new ReturnItemEquivalence(checks(false, true, false, false, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @Test
    void loreMismatchFailsWhenChecked() {
        ItemStack expected = item(Material.DIAMOND_SWORD);
        ItemStack actual = item(Material.DIAMOND_SWORD);

        ItemMeta expectedMeta = mock(ItemMeta.class);
        ItemMeta actualMeta = mock(ItemMeta.class);
        when(expectedMeta.lore()).thenReturn(List.of(Component.text("Enchanted")));
        when(actualMeta.lore()).thenReturn(List.of());
        when(expectedMeta.getEnchants()).thenReturn(Map.of());
        when(actualMeta.getEnchants()).thenReturn(Map.of());
        when(expected.getItemMeta()).thenReturn(expectedMeta);
        when(actual.getItemMeta()).thenReturn(actualMeta);

        assertFalse(new ReturnItemEquivalence(checks(false, false, false, true, false)).test(expected, actual));
        assertTrue(new ReturnItemEquivalence(checks(false, false, false, false, false)).test(expected, actual));
    }

    @Test
    void missingMetaOnBothSidesMatches() {
        ItemStack expected = item(Material.COBBLESTONE);
        ItemStack actual = item(Material.COBBLESTONE);
        when(expected.getItemMeta()).thenReturn(null);
        when(actual.getItemMeta()).thenReturn(null);

        assertTrue(new ReturnItemEquivalence(checks(true, true, true, true, false)).test(expected, actual));
    }
}
