package com.eternalcode.parcellockers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;

/**
 * Real {@link ItemStack}s need a running server (item registry), so persistence tests use mocks
 * serialized to a single marker byte and resolved back through a mocked {@link UnsafeValues}.
 */
public final class TestItemStacks {

    private static final String BUKKIT_SERVER_FIELD = "server";

    private TestItemStacks() {
    }

    public static ItemStack stack(byte marker) {
        ItemStack item = mock(ItemStack.class);
        when(item.isEmpty()).thenReturn(false);
        when(item.serializeAsBytes()).thenReturn(new byte[] { marker });
        return item;
    }

    public static void installDeserializer(Map<Byte, ItemStack> itemsByMarker) throws ReflectiveOperationException {
        Map<Byte, ItemStack> items = Map.copyOf(itemsByMarker);
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.deserializeItem(any(byte[].class))).thenAnswer(invocation -> {
            byte[] serialized = invocation.getArgument(0);
            return items.get(serialized[0]);
        });

        Server server = mock(Server.class);
        when(server.getUnsafe()).thenReturn(unsafe);

        Field serverField = Bukkit.class.getDeclaredField(BUKKIT_SERVER_FIELD);
        serverField.setAccessible(true);
        serverField.set(null, server);
    }
}
