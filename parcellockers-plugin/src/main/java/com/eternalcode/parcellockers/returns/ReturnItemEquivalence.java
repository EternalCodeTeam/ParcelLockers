package com.eternalcode.parcellockers.returns;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Config-driven equivalence between a deposited item and an original parcel item.
 * Material must always match; amounts are compared by {@link ParcelReturnValidator},
 * not here. The relation is symmetric.
 */
public class ReturnItemEquivalence implements ReturnItemComparator {

    private final PluginConfig.ReturnChecks checks;

    public ReturnItemEquivalence(PluginConfig.ReturnChecks checks) {
        this.checks = checks;
    }

    @Override
    public EnumSet<ReturnItemDifference> differences(ItemStack expected, ItemStack deposited) {
        EnumSet<ReturnItemDifference> differences = EnumSet.noneOf(ReturnItemDifference.class);
        if (expected.getType() != deposited.getType()) {
            differences.add(ReturnItemDifference.MATERIAL);
            return differences;
        }

        ItemMeta expectedMeta = expected.getItemMeta();
        ItemMeta depositedMeta = deposited.getItemMeta();
        if (this.checks.checkDurability && damage(expectedMeta) != damage(depositedMeta)) {
            differences.add(ReturnItemDifference.DURABILITY);
        }
        if (this.checks.checkItemName && !Objects.equals(displayName(expectedMeta), displayName(depositedMeta))) {
            differences.add(ReturnItemDifference.ITEM_NAME);
        }
        if (this.checks.checkEnchantments && !enchants(expectedMeta).equals(enchants(depositedMeta))) {
            differences.add(ReturnItemDifference.ENCHANTMENTS);
        }
        if (this.checks.checkLore && !Objects.equals(lore(expectedMeta), lore(depositedMeta))) {
            differences.add(ReturnItemDifference.LORE);
        }
        if (this.checks.checkNbt && !this.residualDataMatches(expected, deposited)) {
            differences.add(ReturnItemDifference.NBT);
        }
        return differences;
    }

    private boolean residualDataMatches(ItemStack expected, ItemStack deposited) {
        return this.normalizeResidualData(expected).isSimilar(this.normalizeResidualData(deposited));
    }

    private ItemStack normalizeResidualData(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        meta.displayName(null);
        List.copyOf(meta.getEnchants().keySet()).forEach(meta::removeEnchant);
        meta.lore(null);
        copy.setItemMeta(meta);
        return copy;
    }

    private static int damage(ItemMeta meta) {
        return meta instanceof Damageable damageable ? damageable.getDamage() : 0;
    }

    private static Component displayName(ItemMeta meta) {
        return meta == null ? null : meta.displayName();
    }

    private static Map<Enchantment, Integer> enchants(ItemMeta meta) {
        return meta == null ? Map.of() : meta.getEnchants();
    }

    private static List<Component> lore(ItemMeta meta) {
        return meta == null ? null : meta.lore();
    }
}
