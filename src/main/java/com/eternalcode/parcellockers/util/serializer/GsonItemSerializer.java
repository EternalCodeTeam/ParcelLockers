package com.eternalcode.parcellockers.util.serializer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GsonItemSerializer {

    private static final String ITEM_STACK_KEY = "itemStack";
    private static final String ITEM_META_KEY = "itemMeta";

    private static final Type DATA_TYPE = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
    private static final Gson GSON = new Gson();

    private static final Class<? extends ConfigurationSerializable> ITEM_META_CLASS;
    private static final Map<String, Function<Object, Object>> SERIALIZERS = new HashMap<>();
    private static final Map<String, Function<Object, Object>> DESERIALIZERS = new HashMap<>();

    static {
        Class<? extends ConfigurationSerializable> itemMetaClass = ConfigurationSerialization.getClassByAlias("ItemMeta");

        if (itemMetaClass == null) {
            throw new IllegalStateException("ItemMeta class not found");
        }

        ITEM_META_CLASS = itemMetaClass;
        SERIALIZERS.put("color", obj -> obj instanceof Color color ? color.serialize() : obj);
        DESERIALIZERS.put("color", color -> {
            if (color instanceof Map<?,?>) {
                return Color.deserialize((Map<String, Object>) color);
            }

            return color;
        });
    }

    public String serialize(ItemStack entity) {
        if (entity == null) {
            return "null";
        }

        ItemStack itemStack = entity.clone();
        ItemMeta meta = itemStack.getItemMeta();
        ConfigurationSerialization.getClassByAlias("ItemMeta");

        itemStack.setItemMeta(null);

        Map<String, Map<String, Object>> data = new HashMap<>();

        if (meta != null) {
            Map<String, Object> serialize = new HashMap<>(meta.serialize());

            for (Map.Entry<String, Function<Object, Object>> entry : SERIALIZERS.entrySet()) {
                serialize.put(entry.getKey(), entry.getValue().apply(serialize.get(entry.getKey())));
            }

            data.put(ITEM_META_KEY, serialize);
        }

        data.put(ITEM_STACK_KEY, itemStack.serialize());

        return GSON.toJson(data);
    }

    public ItemStack deserialize(String value) {
        if (value.equals("null")) {
            return new ItemStack(Material.AIR);
        }

        Map<String, Map<String, Object>> data = GSON.fromJson(value, DATA_TYPE);

        Map<String, Object> itemData = data.get(ITEM_STACK_KEY);
        Map<String, Object> itemMetaData = data.get(ITEM_META_KEY);

        ItemStack itemStack = ItemStack.deserialize(itemData);

        if (itemMetaData != null) {
            for (Map.Entry<String, Object> entry : itemMetaData.entrySet()) {
                Function<Object, Object> deserializer = DESERIALIZERS.get(entry.getKey());

                if (deserializer == null) {
                    continue;
                }

                itemMetaData.put(entry.getKey(), deserializer.apply(entry.getValue()));
            }

            itemStack.setItemMeta((ItemMeta) ConfigurationSerialization.deserializeObject(itemMetaData, ITEM_META_CLASS));
        }

        return itemStack;
    }

}
