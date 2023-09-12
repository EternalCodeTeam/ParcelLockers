package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import static com.eternalcode.parcellockers.util.AdventureUtil.RESET_ITEM;

public class ParcelLockerMainGUI extends GuiView {

    private final MiniMessage miniMessage;
    private final PluginConfiguration config;

    public ParcelLockerMainGUI(MiniMessage miniMessage, PluginConfiguration config) {
        this.miniMessage = miniMessage;
        this.config = config;
    }

    @Override
    public void show(Player player) {
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage, event -> event.getView().close());

        Component guiTitle = this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle));
        
        Gui gui = Gui.gui()
            .title(RESET_ITEM.append(guiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(20, this.config.guiSettings.parcelLockerCollectItem.toGuiItem(this.miniMessage, event -> event.setCancelled(true)));
        gui.setItem(22, this.config.guiSettings.parcelLockerSendItem.toGuiItem(this.miniMessage, event -> new ParcelSendingGUI(this.config, this.miniMessage).show(player)));
        gui.setItem(24, this.config.guiSettings.parcelLockerStatusItem.toGuiItem(this.miniMessage));
        gui.setItem(49, closeItem);

        gui.open(player);
    }
}
