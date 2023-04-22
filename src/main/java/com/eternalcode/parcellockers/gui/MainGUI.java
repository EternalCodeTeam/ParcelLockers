package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelRepository;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MainGUI {

    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository repository;

    public MainGUI(MiniMessage miniMessage, PluginConfiguration config, ParcelRepository repository) {
        this.miniMessage = miniMessage;
        this.config = config;
        this.repository = repository;
    }

    public void showMainGUI(Player player) {
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem myParcelsItem = this.config.guiSettings.myParcelsItem.toGuiItem(this.miniMessage);
        GuiItem sentParcelsItem = this.config.guiSettings.sentParcelsItem.toGuiItem(this.miniMessage);
        GuiItem parcelArchiveItem = this.config.guiSettings.parcelArchiveItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);

        Gui gui = Gui.gui()
                .type(GuiType.CHEST)
                .title(this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle))
                .disableAllInteractions()
                .rows(6)
                .create();

        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }
        gui.setItem(0, cornerItem);
        gui.setItem(8, cornerItem);
        gui.setItem(45, cornerItem);
        gui.setItem(53, cornerItem);

        gui.setItem(20, myParcelsItem);
        gui.setItem(22, sentParcelsItem);
        gui.setItem(24, parcelArchiveItem);
        gui.setItem(40, closeItem);

        gui.addSlotAction(40, event -> event.getView().close());
        gui.addSlotAction(20, event -> {
            event.getView().close();
            new ParcelListGUI(Bukkit.getServer(), this.miniMessage, this.config, this.repository).showParcelListGUI(player);
        });
        gui.addSlotAction(22, event -> {
            event.getView().close();
            new SentParcelsGUI(Bukkit.getServer(), this.miniMessage, this.config, this.repository).showSentParcelsGUI(player);
        });
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.open(player);

    }
}
