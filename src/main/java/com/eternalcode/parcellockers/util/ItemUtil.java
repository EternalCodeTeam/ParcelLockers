package com.eternalcode.parcellockers.util;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.eldoria.jacksonbukkit.JacksonPaper;
import io.sentry.Sentry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;

public class ItemUtil {

    private static final ObjectMapper JSON = JsonMapper.builder()
        .addModule(JacksonPaper.builder()
            .useLegacyItemStackSerialization()
            .build()
        )
        .build();

    private ItemUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean compareMeta(ItemStack first, ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        if (firstMeta == null) {
            return false;
        }
        
        ItemMeta secondMeta = second.getItemMeta();
        if (secondMeta == null) {
            return false;
        }

        if (first.getType() != second.getType()) {
            return false;
        }
        
        return new HashSet<>(firstMeta.getLore()).containsAll(secondMeta.getLore())
            && firstMeta.getDisplayName().equals(secondMeta.getDisplayName());
    }

    public static String serialize(ItemStack stack) {
        try {
            return JSON.writeValueAsString(stack);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to serialize itemstack", e);
        }
    }

    public static ItemStack deserialize(String string) {
        try {
            return JSON.readValue(string, ItemStack.class);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to deserialize itemstack", e);
        }
    }

}
