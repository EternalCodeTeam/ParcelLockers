package com.eternalcode.parcellockers.returns;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public record ReturnItemMismatch(
    ReturnMismatchType type,
    Material item,
    int expectedAmount,
    int depositedAmount,
    Integer expectedDamage,
    Integer depositedDamage
) {

    public ReturnItemMismatch {
        Objects.requireNonNull(type, "Mismatch type cannot be null");
        Objects.requireNonNull(item, "Mismatch item cannot be null");
    }

    public static ReturnItemMismatch unexpected(Material item, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.UNEXPECTED_ITEM, item, 0, deposited, null, null);
    }

    public static ReturnItemMismatch insufficient(Material item, int expected, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.INSUFFICIENT_AMOUNT, item, expected, deposited, null, null);
    }

    public static ReturnItemMismatch excess(Material item, int expected, int deposited) {
        return new ReturnItemMismatch(ReturnMismatchType.EXCESS_AMOUNT, item, expected, deposited, null, null);
    }

    public static ReturnItemMismatch attribute(ReturnMismatchType type, ItemStack expected, ItemStack deposited) {
        if (type == ReturnMismatchType.UNEXPECTED_ITEM
            || type == ReturnMismatchType.INSUFFICIENT_AMOUNT
            || type == ReturnMismatchType.EXCESS_AMOUNT) {
            throw new IllegalArgumentException("Amount mismatch cannot be created as an attribute mismatch");
        }
        Integer expectedDamage = type == ReturnMismatchType.DURABILITY ? damage(expected) : null;
        Integer depositedDamage = type == ReturnMismatchType.DURABILITY ? damage(deposited) : null;
        return new ReturnItemMismatch(type, expected.getType(), 0, 0, expectedDamage, depositedDamage);
    }

    private static int damage(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable damageable ? damageable.getDamage() : 0;
    }
}
