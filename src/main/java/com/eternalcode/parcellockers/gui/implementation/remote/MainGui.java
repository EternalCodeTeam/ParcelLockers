package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.user.UserManager;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

public class MainGui implements GuiView {

    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final UserManager userManager;

    public MainGui(
        Plugin plugin,
        Server server,
        MiniMessage miniMessage,
        PluginConfiguration config,
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        UserManager userManager
    ) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.userManager = userManager;
    }

    @Override
    public void show(Player player) {
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;

        Gui gui = Gui.gui()
            .title(resetItalic(this.miniMessage.deserialize(guiSettings.mainGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem myParcelsItem = guiSettings.myParcelsItem.toGuiItem();
        GuiItem sentParcelsItem = guiSettings.sentParcelsItem.toGuiItem();
        GuiItem parcelArchiveItem = guiSettings.parcelArchiveItem.toGuiItem();
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();

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

        gui.addSlotAction(20, event -> new ParcelListGui(this.plugin, this.server, this.miniMessage, this.config, this.parcelRepository, this.lockerRepository, this.userManager, this).show(player));
        gui.addSlotAction(22, event -> new SentParcelsGui(this.plugin, this.server, this.miniMessage, this.config, this.parcelRepository, this.lockerRepository, this, this.userManager).show(player));
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.open(player);

    }
}
