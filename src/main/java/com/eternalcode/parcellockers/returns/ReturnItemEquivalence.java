package com.eternalcode.parcellockers.returns;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
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
public class ReturnItemEquivalence implements BiPredicate<ItemStack, ItemStack> {

    private final PluginConfig.ReturnChecks checks;

    public ReturnItemEquivalence(PluginConfig.ReturnChecks checks) {
        this.checks = checks;
    }

    @Override
    public boolean test(ItemStack expected, ItemStack actual) {
        if (expected.getType() != actual.getType()) {
            return false;
        }
        if (this.checks.checkNbt) {
            if (this.allAttributeChecksEnabled()) {
                return expected.isSimilar(actual);
            }
            return this.normalize(expected).isSimilar(this.normalize(actual));
        }
        return this.attributesMatch(expected.getItemMeta(), actual.getItemMeta());
    }

    private boolean allAttributeChecksEnabled() {
        return this.checks.checkDurability
            && this.checks.checkItemName
            && this.checks.checkEnchantments
            && this.checks.checkLore;
    }

    /** Strips the attributes whose check is disabled so isSimilar ignores them. */
    private ItemStack normalize(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        if (!this.checks.checkDurability && meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        if (!this.checks.checkItemName) {
            meta.displayName(null);
        }
        if (!this.checks.checkEnchantments) {
            meta.getEnchants().keySet().forEach(meta::removeEnchant);
        }
        if (!this.checks.checkLore) {
            meta.lore(null);
        }
        copy.setItemMeta(meta);
        return copy;
    }

    /** checkNbt = false: compare only the enabled attributes. */
    private boolean attributesMatch(ItemMeta expected, ItemMeta actual) {
        if (this.checks.checkDurability && damage(expected) != damage(actual)) {
            return false;
        }
        if (this.checks.checkItemName && !Objects.equals(displayName(expected), displayName(actual))) {
            return false;
        }
        if (this.checks.checkEnchantments && !enchants(expected).equals(enchants(actual))) {
            return false;
        }
        if (this.checks.checkLore && !Objects.equals(lore(expected), lore(actual))) {
            return false;
        }
        return true;
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
