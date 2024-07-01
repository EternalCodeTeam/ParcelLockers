package com.eternalcode.parcellockers.itemstorage;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public record ItemStorage(UUID owner, List<ItemStack> items) {

}
