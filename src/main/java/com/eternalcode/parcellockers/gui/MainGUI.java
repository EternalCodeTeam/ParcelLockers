package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static com.eternalcode.parcellockers.util.AdventureUtil.RESET_ITEM;

public class MainGUI extends GuiView {

    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;
    private final ParcelLockerRepository parcelLockerRepository;

    public MainGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepository parcelRepository, ParcelLockerRepository parcelLockerRepository) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.parcelLockerRepository = parcelLockerRepository;
    }

    @Override
    public void show(Player player) {

        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem myParcelsItem = guiSettings.myParcelsItem.toGuiItem(this.miniMessage);
        GuiItem sentParcelsItem = guiSettings.sentParcelsItem.toGuiItem(this.miniMessage);
        GuiItem parcelArchiveItem = guiSettings.parcelArchiveItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(this.miniMessage, event -> event.getView().close());
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem(this.miniMessage);

        Gui gui = Gui.gui()
                .type(GuiType.CHEST)
                .title(RESET_ITEM.append(this.miniMessage.deserialize(guiSettings.mainGuiTitle)))
                .disableAllInteractions()
                .rows(6)
                .create();

        int size = gui.getRows() * 9;
        for (int i = 0; i < size; i++) {
            gui.setItem(i, backgroundItem);
        }

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        gui.setItem(20, myParcelsItem);
        gui.setItem(22, sentParcelsItem);
        gui.setItem(24, parcelArchiveItem);
        gui.setItem(40, closeItem);

        gui.addSlotAction(20, event -> new ParcelListGUI(this.plugin, this.miniMessage, this.config, this.parcelRepository, this.parcelLockerRepository, this).show(player));
        gui.addSlotAction(22, event -> {
            event.getView().close();
            new SentParcelsGUI(this.plugin, this.server, this.miniMessage, this.config, this.parcelRepository, this.parcelLockerRepository, this).show(player);
        });
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.open(player);

    }
}
