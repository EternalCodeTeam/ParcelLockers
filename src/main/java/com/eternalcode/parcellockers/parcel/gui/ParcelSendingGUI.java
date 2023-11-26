package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.gui.LockerMainGUI;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public class ParcelSendingGUI extends GuiView {

    private final Plugin plugin;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;
    private ParcelSize size;
    private boolean priority;

    public ParcelSendingGUI(Plugin plugin, PluginConfiguration config, MiniMessage miniMessage, ItemStorageRepository itemStorageRepository) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
    }

    @Override
    public void show(Player player) {
        PluginConfiguration.GuiSettings settings = this.config.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(settings.parcelLockerSendingGuiTitle);

        Gui gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem backgroundItem = settings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = settings.cornerItem.toGuiItem();
        GuiItem storageItem = settings.parcelStorageItem.toGuiItem(event -> {
            ParcelItemStorageGUI storageGUI = new ParcelItemStorageGUI(plugin, this.config, this.miniMessage, itemStorageRepository);
            storageGUI.show(player, this.size);
        });

        GuiItem closeItem = settings.closeItem.toGuiItem(event -> new LockerMainGUI(plugin, this.miniMessage, this.config, itemStorageRepository).show(player));

        ConfigItem smallButton = settings.smallParcelSizeItem;
        ConfigItem mediumButton = settings.mediumParcelSizeItem;
        ConfigItem largeButton = settings.largeParcelSizeItem;
        ConfigItem priorityItem = settings.priorityItem;

        GuiAction<InventoryClickEvent> priorityItemAction = event -> {
            this.priority = !this.priority;
            gui.updateItem(31, priorityItem.toBuilder().glow(this.priority).build());
        };

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.setSelected(gui, ParcelSize.SMALL);
        this.priority = false;

        gui.setItem(20, smallButton.toGuiItem(event -> setSelected(gui, ParcelSize.SMALL)));
        gui.setItem(22, mediumButton.toGuiItem(event -> setSelected(gui, ParcelSize.MEDIUM)));
        gui.setItem(24, largeButton.toGuiItem(event -> setSelected(gui, ParcelSize.LARGE)));
        gui.setItem(31, priorityItem.toGuiItem(priorityItemAction));
        gui.setItem(37, storageItem);
        gui.setItem(40, closeItem);

        gui.open(player);
    }

    private void setSelected(Gui gui, ParcelSize size) {
        PluginConfiguration.GuiSettings settings = config.guiSettings;
        this.size = size;

        ConfigItem smallButton = size == ParcelSize.SMALL ? settings.selectedSmallParcelSizeItem : settings.smallParcelSizeItem;
        ConfigItem mediumButton = size == ParcelSize.MEDIUM ? settings.selectedMediumParcelSizeItem : settings.mediumParcelSizeItem;
        ConfigItem largeButton = size == ParcelSize.LARGE ? settings.selectedLargeParcelSizeItem : settings.largeParcelSizeItem;

        gui.updateItem(20, smallButton.toItemStack());
        gui.updateItem(22, mediumButton.toItemStack());
        gui.updateItem(24, largeButton.toItemStack());
    }

}
