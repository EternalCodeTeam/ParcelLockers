package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelItemStorageGUI {

    private final PluginConfiguration config;
    private final MiniMessage miniMessage;

    private StorageGui gui;
    private boolean confirmed;

    public ParcelItemStorageGUI(PluginConfiguration config, MiniMessage miniMessage) {
        this.config = config;
        this.miniMessage = miniMessage;
    }

    void show(Player player, ParcelSize size) {
        GuiItem confirmItem = this.config.guiSettings.confirmItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = true;
            event.getView().close();
            new ParcelSendingGUI(this.config, this.miniMessage).show(player);
        });
        GuiItem cancelItem = this.config.guiSettings.cancelItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = false;
            event.getView().close();
            new ParcelSendingGUI(this.config, this.miniMessage).show(player);
        });

        switch (size) {
            case SMALL -> this.gui = Gui.storage()
                .title(Component.text(this.config.guiSettings.parcelSmallContentGuiTitle))
                .rows(1)
                .create();
            case MEDIUM -> this.gui = Gui.storage()
                .title(Component.text(this.config.guiSettings.parcelMediumContentGuiTitle))
                .rows(2)
                .create();
            case LARGE -> this.gui = Gui.storage()
                .title(Component.text(this.config.guiSettings.parcelLargeContentGuiTitle))
                .rows(3)
                .create();
        }
        this.gui.setCloseGuiAction(event -> {
            if (this.confirmed) {
                if (gui.getInventory().getContents() == null) {
                    return;
                }
                for (ItemStack item : this.gui.getInventory().getContents()) {
                    player.getInventory().addItem(item);
                    gui.removeItem(item);
                }
            }
        });
        this.gui.open(player);

    }
}
