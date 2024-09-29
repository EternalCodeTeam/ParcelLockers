package com.eternalcode.parcellockers.configuration.implementation;


import com.eternalcode.commons.adventure.AdventureLegacyColorPostProcessor;
import com.eternalcode.commons.adventure.AdventureUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import net.dzikoysk.cdn.entity.Contextual;
import net.dzikoysk.cdn.entity.Exclude;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Contextual
public class ConfigItem implements Cloneable {

    @Exclude
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .postProcessor(new AdventureLegacyColorPostProcessor())
        .build();

    public Material type = Material.STONE;
    public String name = "&fItem name";
    public List<String> lore = List.of("&fFirst line of lore", "&9Second line of lore");
    public boolean glow = false;

    public GuiItem toGuiItem(GuiAction<InventoryClickEvent> action) {
        return ItemBuilder.from(this.type)
            .name(AdventureUtil.resetItalic(MINI_MESSAGE.deserialize(this.name)))
            .lore(this.lore.stream().map(element -> AdventureUtil.resetItalic(MINI_MESSAGE.deserialize(element))).toList())
            .flags(ItemFlag.HIDE_ATTRIBUTES)
            .flags(ItemFlag.HIDE_ENCHANTS)
            .glow(this.glow)
            .asGuiItem(action);
    }

    public @NotNull ItemBuilder toBuilder() {
        return ItemBuilder.from(this.type)
            .name(AdventureUtil.resetItalic(MINI_MESSAGE.deserialize(this.name)))
            .lore(this.lore.stream().map(element -> AdventureUtil.resetItalic(MINI_MESSAGE.deserialize(element))).toList())
            .flags(ItemFlag.HIDE_ATTRIBUTES)
            .flags(ItemFlag.HIDE_ENCHANTS)
            .glow(this.glow);
    }

    public GuiItem toGuiItem() {
        return this.toGuiItem(event -> {});
    }

    public ItemStack toItemStack() {
        return this.toBuilder().build();
    }

    public Material getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public boolean isGlow() {
        return this.glow;
    }

    public ConfigItem setType(Material type) {
        this.type = type;
        return this;
    }

    public ConfigItem setName(String name) {
        this.name = name;
        return this;
    }

    public ConfigItem setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ConfigItem addLore(String lore) {
        this.lore.add(lore);
        return this;
    }

    public ConfigItem setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }

    @Override
    public ConfigItem clone() {
        try {
            ConfigItem cloned = (ConfigItem) super.clone();
            cloned.lore = new ArrayList<>(this.lore); // Deep copy of the list
            return cloned;
        }
        catch (CloneNotSupportedException e) {
            throw new AssertionError("Failed to clone " + this); // Should never happen
        }
    }
}
