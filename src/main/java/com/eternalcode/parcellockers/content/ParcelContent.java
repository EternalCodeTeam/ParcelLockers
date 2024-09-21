package com.eternalcode.parcellockers.content;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public record ParcelContent(UUID uniqueId, List<ItemStack> items) {

}
