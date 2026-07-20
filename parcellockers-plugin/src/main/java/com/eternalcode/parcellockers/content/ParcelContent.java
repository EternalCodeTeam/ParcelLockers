package com.eternalcode.parcellockers.content;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public record ParcelContent(UUID uniqueId, List<ItemStack> items) {

    public ParcelContent {
        // Guard against null/empty stacks leaking into the content (issue #221): empty/air slots
        // can slip past the GUI write filters and round-trip through the persister as nulls, which
        // would later NPE when the collection GUI reads itemStack.getType().
        items = items == null || items.isEmpty()
            ? List.of()
            : items.stream()
                .filter(item -> item != null && !item.isEmpty())
                .toList();
    }
}
