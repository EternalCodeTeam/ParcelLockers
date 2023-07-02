package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static com.eternalcode.parcellockers.util.AdventureUtil.RESET_ITEM;

public class ParcelLockerMainGUI extends GuiView {

    private final MiniMessage miniMessage;
    private final Plugin plugin;
    private final ParcelRepository parcelRepository;
    private final ParcelLockerRepository parcelLockerRepository;
    private final PluginConfiguration config;

    public ParcelLockerMainGUI(MiniMessage miniMessage, Plugin plugin, ParcelRepository parcelRepository, ParcelLockerRepository parcelLockerRepository, PluginConfiguration config) {
        this.miniMessage = miniMessage;
        this.plugin = plugin;
        this.parcelRepository = parcelRepository;
        this.parcelLockerRepository = parcelLockerRepository;
        this.config = config;
    }

    @Override
    public void show(Player player) {
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage, event -> event.getView().close());

        Gui gui = Gui.gui()
            .title(RESET_ITEM.append(this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int i = 0; i < gui.getRows() * 9 - 1; i++) {
            gui.setItem(i, backgroundItem);
        }

        gui.setItem(20, this.config.guiSettings.parcelLockerCollectItem.toGuiItem(this.miniMessage, event -> new ParcelListGUI(this.plugin, this.miniMessage, this.config, this.parcelRepository, this.parcelLockerRepository).show(player)));
        gui.setItem(22, this.config.guiSettings.parcelLockerSendItem.toGuiItem(this.miniMessage));
        gui.setItem(24, this.config.guiSettings.parcelLockerStatusItem.toGuiItem(this.miniMessage));
        gui.setItem(49, closeItem);

        gui.open(player);
    }
}
