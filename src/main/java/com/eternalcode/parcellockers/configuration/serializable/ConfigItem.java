package com.eternalcode.parcellockers.configuration.serializable;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.adventure.AdventureLegacyColorPostProcessor;
import com.eternalcode.commons.adventure.AdventureLegacyColorPreProcessor;
import com.eternalcode.parcellockers.nexo.NexoIntegration;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Comment("# Optional: Nexo item ID. If set, this will override the 'type' field with a Nexo custom item.")
    private String nexoId = "";

    public PaperItemBuilder toBuilder() {
        ItemStack baseItem = this.getBaseItemStack();

        return PaperItemBuilder.from(baseItem)
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

    @ApiStatus.Internal
    public ItemStack toRawItemStack() {
        ItemStack itemStack = this.getBaseItemStack();

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

    private ItemStack getBaseItemStack() {
        if (this.nexoId != null && !this.nexoId.isEmpty()) {
            Optional<ItemStack> nexoItem = NexoIntegration.getItemStack(this.nexoId);
            if (nexoItem.isPresent()) {
                return nexoItem.get();
            }
        }
        return new ItemStack(this.type);
    }

    public boolean isNexoItem() {
        return this.nexoId != null && !this.nexoId.isEmpty();
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        if (this.isNexoItem()) {
            return NexoIntegration.matches(itemStack, this.nexoId);
        }

        return this.toRawItemStack().isSimilar(itemStack);
    }

    public ConfigItem nexoId(String nexoId) {
        this.nexoId = nexoId;
        return this;
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
