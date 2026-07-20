package com.eternalcode.parcellockers.configuration.serializable;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.adventure.AdventureLegacyColorPostProcessor;
import com.eternalcode.commons.adventure.AdventureLegacyColorPreProcessor;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import eu.okaeri.configs.annotation.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@Getter
@Setter
@Accessors(fluent = true)
public class ConfigItem implements Serializable, Cloneable {

    @Exclude
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .preProcessor(new AdventureLegacyColorPreProcessor())
        .postProcessor(new AdventureLegacyColorPostProcessor())
        .build();

    private Material type = Material.STONE;
    private String name = "";
    private List<String> lore = List.of();
    private boolean glow = false;

    public PaperItemBuilder toBuilder() {
        return PaperItemBuilder.from(this.type)
            .name(resetItalic(MINI_MESSAGE.deserialize(this.name)))
            .lore(this.lore.stream().map(element -> resetItalic(MINI_MESSAGE.deserialize(element))).toList())
            .flags(ItemFlag.HIDE_ENCHANTS)
            .glow(this.glow);
    }

    public GuiItem toGuiItem() {
        return this.toBuilder().asGuiItem();
    }

    public GuiItem toGuiItem(GuiAction<InventoryClickEvent> action) {
        return this.toBuilder().asGuiItem(action);
    }

    public ItemStack toItemStack() {
        return this.toBuilder().build();
    }

    /**
     * By default, ConfigItem#toItemStack adds a "parcellockers-mfgui" value to the item's PersistentDataContainer
     * to identify it as a GUI item.
     * This method, on the other hand creates a raw ItemStack without that extra data.
     */

    @ApiStatus.Internal
    public ItemStack toRawItemStack() {
        ItemStack itemStack = new ItemStack(this.type);
        itemStack.editMeta(meta -> {
            meta.displayName(resetItalic(MINI_MESSAGE.deserialize(this.name)));
            meta.lore(this.lore.stream()
                .map(element -> resetItalic(MINI_MESSAGE.deserialize(element)))
                .toList());
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if (this.glow) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return itemStack;
    }

    @Override
    public ConfigItem clone() {
        try {
            ConfigItem cloned = (ConfigItem) super.clone();
            cloned.lore = new ArrayList<>(this.lore);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Failed to clone " + this);
        }
    }
}
