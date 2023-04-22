package com.eternalcode.parcellockers.configuration.implementation;


import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import net.dzikoysk.cdn.entity.Contextual;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

@Contextual
public class ConfigItem {

    public Material type = Material.STONE;
    public String name = "&fItem name";
    public List<String> lore = List.of("&fFirst line of lore", "&9Second line of lore");
    public boolean glow = false;

    public GuiItem toGuiItem(MiniMessage miniMessage) {
        return ItemBuilder.from(this.type)
                .name(miniMessage.deserialize(this.name))
                .lore(this.lore.stream().map(miniMessage::deserialize).toList())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .flags(ItemFlag.HIDE_ENCHANTS)
                .asGuiItem();
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

    public ConfigItem setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }
}
