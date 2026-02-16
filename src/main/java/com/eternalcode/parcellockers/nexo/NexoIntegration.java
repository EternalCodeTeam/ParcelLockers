package com.eternalcode.parcellockers.nexo;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public class NexoIntegration {

    private static Boolean cachedResult;

    private NexoIntegration() {
    }

    public static boolean isEnabled() {
        if (cachedResult != null) {
            return cachedResult;
        }

        return isNexoPresent();
    }

    public static Optional<ItemStack> getItemStack(String nexoId) {
        if (!isEnabled() || nexoId == null || nexoId.isEmpty()) {
            return Optional.empty();
        }

        try {
            ItemBuilder itemBuilder = NexoItems.itemFromId(nexoId);
            if (itemBuilder == null) {
                return Optional.empty();
            }
            return Optional.of(itemBuilder.build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getNexoId(ItemStack itemStack) {
        if (!isEnabled() || itemStack == null) {
            return Optional.empty();
        }

        try {
            String id = NexoItems.idFromItem(itemStack);
            return Optional.ofNullable(id);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean matches(ItemStack itemStack, String nexoId) {
        if (itemStack == null || nexoId == null || nexoId.isEmpty()) {
            return false;
        }

        return getNexoId(itemStack)
            .map(id -> id.equals(nexoId))
            .orElse(false);
    }

    private static synchronized boolean isNexoPresent() {
        try {
            Class.forName("com.nexomc.nexo.api.NexoItems");
            cachedResult = true;
        } catch (ClassNotFoundException e) {
            cachedResult = false;
        }
        return cachedResult;
    }
}

