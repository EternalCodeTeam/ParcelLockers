package com.eternalcode.parcellockers.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ParcelContentTest {

    @Test
    void dropsNullItems() {
        // Reproduces issue #221: a null element in the content list makes CollectionGui
        // NPE on itemStack.getType(). The model must never expose null items.
        ItemStack stone = mock(ItemStack.class);

        ParcelContent content = new ParcelContent(UUID.randomUUID(), Arrays.asList(stone, null));

        assertEquals(List.of(stone), content.items());
    }

    @Test
    void dropsEmptyItems() {
        // Empty/air slots can slip past the GUI write filters; they must not be exposed
        // as content, otherwise they round-trip through the persister as nulls.
        ItemStack stone = mock(ItemStack.class);
        ItemStack air = mock(ItemStack.class);
        when(air.isEmpty()).thenReturn(true);

        ParcelContent content = new ParcelContent(UUID.randomUUID(), List.of(stone, air));

        assertEquals(List.of(stone), content.items());
    }
}
