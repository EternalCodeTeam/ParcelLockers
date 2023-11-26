package com.eternalcode.parcellockers.locker.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.parcel.gui.ParcelSendingGUI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static com.eternalcode.parcellockers.util.AdventureUtil.RESET_ITEM;

public class LockerMainGUI extends GuiView {

    private final Plugin plugin;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ItemStorageRepository itemStorageRepository;

    public LockerMainGUI(Plugin plugin, MiniMessage miniMessage, PluginConfiguration config, ItemStorageRepository itemStorageRepository) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
        this.config = config;
        this.itemStorageRepository = itemStorageRepository;
    }

    @Override
    public void show(Player player) {
        Component guiTitle = this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle);
        
        Gui gui = Gui.gui()
            .title(RESET_ITEM.append(guiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> gui.close(player));

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(20, this.config.guiSettings.parcelLockerCollectItem.toGuiItem(event -> event.setCancelled(true)));
        gui.setItem(22, this.config.guiSettings.parcelLockerSendItem.toGuiItem(event -> new ParcelSendingGUI(plugin, this.config, this.miniMessage, itemStorageRepository).show(player)));
        gui.setItem(24, this.config.guiSettings.parcelLockerStatusItem.toGuiItem());
        gui.setItem(49, closeItem);

        gui.open(player);
    }
}
