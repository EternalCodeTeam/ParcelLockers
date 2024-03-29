package com.eternalcode.parcellockers.locker.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
import com.eternalcode.parcellockers.parcel.gui.ParcelListGUI;
import com.eternalcode.parcellockers.parcel.gui.SentParcelsGUI;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
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
    private final ParcelRepositoryImpl parcelRepository;
    private final LockerRepositoryImpl lockerRepository;

    public MainGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepositoryImpl parcelRepository, LockerRepositoryImpl lockerRepository) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
    }

    @Override
    public void show(Player player) {
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;

        Gui gui = Gui.gui()
            .type(GuiType.CHEST)
            .title(RESET_ITEM.append(this.miniMessage.deserialize(guiSettings.mainGuiTitle)))
            .disableAllInteractions()
            .rows(6)
            .create();

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem myParcelsItem = guiSettings.myParcelsItem.toGuiItem(this.miniMessage);
        GuiItem sentParcelsItem = guiSettings.sentParcelsItem.toGuiItem(this.miniMessage);
        GuiItem parcelArchiveItem = guiSettings.parcelArchiveItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(this.miniMessage, event -> gui.close(player));
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem(this.miniMessage);


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

        gui.addSlotAction(20, event -> new ParcelListGUI(this.plugin, this.server, this.miniMessage, this.config, this.parcelRepository, this.lockerRepository, this).show(player));
        gui.addSlotAction(22, event -> {
            event.getView().close();
            new SentParcelsGUI(this.plugin, this.server, this.miniMessage, this.config, this.parcelRepository, this.lockerRepository, this).show(player);
        });
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.open(player);

    }
}
