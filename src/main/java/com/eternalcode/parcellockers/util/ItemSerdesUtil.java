package com.eternalcode.parcellockers.util;

import com.eternalcode.parcellockers.shared.ParcelLockersException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.eldoria.jacksonbukkit.JacksonPaper;
import io.sentry.Sentry;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class ItemSerdesUtil {

    private static final ObjectMapper JSON = JsonMapper.builder()
        .addModule(JacksonPaper.builder()
            .useLegacyItemStackSerialization()
            .build()
        )
        .build();

    private ItemSerdesUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String serialize(ItemStack stack) {
        try {
            return JSON.writeValueAsString(stack);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to serialize itemstack", e);
        }
    }

    public static String serializeItems(List<ItemStack> stack) {
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

    public static List<ItemStack> deserializeItems(String string) {
        try {
            return JSON.readValue(string, JSON.getTypeFactory().constructCollectionType(List.class, ItemStack.class));
        } catch (JsonProcessingException exception) {
            Sentry.captureException(exception);
            throw new ParcelLockersException("Failed to deserialize itemstack", exception);
        }
    }
}
