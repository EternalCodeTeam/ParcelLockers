package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class ParcelCollectionGui implements GuiView {

    private final Plugin plugin;
    private final PluginConfiguration config;
    private final BukkitScheduler scheduler;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final MiniMessage miniMessage;

    public ParcelCollectionGui(Plugin plugin, PluginConfiguration config, BukkitScheduler scheduler, ParcelRepository parcelRepository, LockerRepository lockerRepository, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.config = config;
        this.scheduler = scheduler;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.miniMessage = miniMessage;
    }

    @Override
    public void show(Player player) {
        PluginConfiguration settings = this.config;
        PluginConfiguration.GuiSettings guiSettings = settings.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(guiSettings.parcelCollectionGuiTitle);

        Gui gui = Gui.gui()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        GuiItem parcelItem = guiSettings.parcelCollectionItem.toGuiItem();
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
    }
}
