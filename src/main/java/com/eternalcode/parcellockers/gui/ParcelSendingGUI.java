package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ParcelSendingGUI extends GuiView {

    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private ParcelSize size;
    private boolean priority;

    public ParcelSendingGUI(PluginConfiguration config, MiniMessage miniMessage) {
        this.config = config;
        this.miniMessage = miniMessage;
    }

    @Override
    void show(Player player) {

        Component guiTitle = this.miniMessage.deserialize(this.config.guiSettings.parcelLockerSendingGuiTitle);

        Gui gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem storageItem = this.config.guiSettings.parcelStorageItem.toGuiItem(this.miniMessage, event -> {
            event.getView().close();
            new ParcelItemStorageGUI(this.config, this.miniMessage).show(player, this.size);
        });
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage, event -> {
            event.getView().close();
            new ParcelLockerMainGUI(this.miniMessage, this.config).show(player);
        });

        ConfigItem smallButton = this.config.guiSettings.smallParcelSizeItem;
        ConfigItem mediumButton = this.config.guiSettings.mediumParcelSizeItem;
        ConfigItem largeButton = this.config.guiSettings.largeParcelSizeItem;
        ConfigItem priorityItem = this.config.guiSettings.priorityItem;

        GuiAction<InventoryClickEvent> smallButtonAction = event -> {
            this.size = ParcelSize.SMALL;
            gui.updateItem(20, smallButton.setGlow(true).toGuiItem(this.miniMessage));
            gui.updateItem(22, mediumButton.setGlow(false).toGuiItem(this.miniMessage));
            gui.updateItem(24, largeButton.setGlow(false).toGuiItem(this.miniMessage));
        };

        GuiAction<InventoryClickEvent> mediumButtonAction = event -> this.updateMediumButton(gui, smallButton, mediumButton, largeButton);

        GuiAction<InventoryClickEvent> largeButtonAction = event -> this.updateSmallButton(gui, smallButton, mediumButton, largeButton);

        GuiAction<InventoryClickEvent> priorityItemAction = event -> this.updatePriorityButton(gui, priorityItem);

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.size = ParcelSize.SMALL;
        this.priority = false;

        gui.setItem(20, smallButton.setGlow(true).toGuiItem(this.miniMessage, smallButtonAction));
        gui.setItem(22, mediumButton.toGuiItem(this.miniMessage, mediumButtonAction));
        gui.setItem(24, largeButton.toGuiItem(this.miniMessage, largeButtonAction));
        gui.setItem(31, priorityItem.toGuiItem(this.miniMessage, priorityItemAction));
        gui.setItem(37, storageItem);
        gui.setItem(40, closeItem);

        gui.open(player);
    }

    private void updateSmallButton(Gui gui, ConfigItem smallButton, ConfigItem mediumButton, ConfigItem largeButton) {
        this.size = ParcelSize.SMALL;
        
        gui.updateItem(20, smallButton.setGlow(true).toGuiItem(this.miniMessage));
        gui.updateItem(22, mediumButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateMediumButton(gui, smallButton, mediumButton, largeButton)));
        gui.updateItem(24, largeButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateLargeButton(gui, smallButton, mediumButton, largeButton)));
    }

    private void updateMediumButton(Gui gui, ConfigItem smallButton, ConfigItem mediumButton, ConfigItem largeButton) {
        this.size = ParcelSize.MEDIUM;
        
        gui.updateItem(20, smallButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateSmallButton(gui, smallButton, mediumButton, largeButton)));
        gui.updateItem(22, mediumButton.setGlow(true).toGuiItem(this.miniMessage));
        gui.updateItem(24, largeButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateLargeButton(gui, smallButton, mediumButton, largeButton)));
    }

    private void updateLargeButton(Gui gui, ConfigItem smallButton, ConfigItem mediumButton, ConfigItem largeButton) {
        this.size = ParcelSize.LARGE;
        
        gui.updateItem(20, smallButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateSmallButton(gui, smallButton, mediumButton, largeButton)));
        gui.updateItem(22, mediumButton.setGlow(false).toGuiItem(this.miniMessage, e -> updateMediumButton(gui, smallButton, mediumButton, largeButton)));
        gui.updateItem(24, largeButton.setGlow(true).toGuiItem(this.miniMessage));
    }

    private void updatePriorityButton(Gui gui, ConfigItem priorityItem) {
        this.priority = !this.priority;
        gui.updateItem(31, priorityItem.setGlow(this.priority).toGuiItem(this.miniMessage));
    }
}
