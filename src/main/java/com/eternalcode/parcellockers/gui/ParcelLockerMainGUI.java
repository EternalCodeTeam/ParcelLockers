package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import dev.triumphteam.gui.components.GuiType;
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

        Gui gui = Gui.gui()
            .title(RESET_ITEM.append(this.miniMessage.deserialize(this.config.guiSettings.mainGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .type(GuiType.CHEST)
            .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int i = 0; i < gui.getRows() * 9 - 1; i++) {
            gui.setItem(i, backgroundItem);
        }



    }
}
