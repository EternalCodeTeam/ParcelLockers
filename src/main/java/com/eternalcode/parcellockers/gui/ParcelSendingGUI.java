package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParcelSendingGUI extends GuiView {

    private final Map<UUID, ParcelSize> sizeMap = new HashMap<>();
    private final Map<UUID, Boolean> priorityMap = new HashMap<>();
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;

    public ParcelSendingGUI(PluginConfiguration config, MiniMessage miniMessage) {
        this.config = config;
        this.miniMessage = miniMessage;
    }

    @Override
    void show(Player player) {
        UUID uuid = player.getUniqueId();
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage, event -> event.getView().close());

        ConfigItem smallButton = this.config.guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = this.config.guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = this.config.guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = this.config.guiSettings.priorityItem;


        Gui gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(this.miniMessage.deserialize(this.config.guiSettings.parcelLockerSendingGuiTitle))
            .create();

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.sizeMap.put(uuid, ParcelSize.SMALL);
        this.priorityMap.put(uuid, false);

        gui.setItem(20, smallButton.setGlow(true).toGuiItem(this.miniMessage, event -> {
            this.sizeMap.put(uuid, ParcelSize.SMALL);
            gui.updateItem(20, smallButton.setGlow(true).toGuiItem(this.miniMessage));
            gui.updateItem(22, mediumButton.setGlow(false).toGuiItem(this.miniMessage));
            gui.updateItem(24, largeButton.setGlow(false).toGuiItem(this.miniMessage));
        }));

        gui.setItem(22, mediumButton.toGuiItem(this.miniMessage, event -> {
            this.sizeMap.put(uuid, ParcelSize.MEDIUM);
            gui.updateItem(20, smallButton.setGlow(false).toGuiItem(this.miniMessage));
            gui.updateItem(22, mediumButton.setGlow(true).toGuiItem(this.miniMessage));
            gui.updateItem(24, largeButton.setGlow(false).toGuiItem(this.miniMessage));
        }));

        gui.setItem(24, largeButton.toGuiItem(this.miniMessage, event -> {
            this.sizeMap.put(uuid, ParcelSize.LARGE);
            gui.updateItem(20, smallButton.setGlow(false).toGuiItem(this.miniMessage));
            gui.updateItem(22, mediumButton.setGlow(false).toGuiItem(this.miniMessage));
            gui.updateItem(24, largeButton.setGlow(true).toGuiItem(this.miniMessage));
        }));

        gui.setItem(31, priorityItem.toGuiItem(this.miniMessage, event -> {
            this.priorityMap.put(uuid, !this.priorityMap.getOrDefault(uuid, false));
            gui.updateItem(31, priorityItem.setGlow(!priorityItem.glow).toGuiItem(this.miniMessage));
        }));
        gui.setItem(40, closeItem);

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.setCloseGuiAction(event -> this.sizeMap.remove(uuid));
    }
}
