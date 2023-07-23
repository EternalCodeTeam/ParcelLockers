package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelItemStorageGUI {

    private final PluginConfiguration config;

    private StorageGui gui;

    public ParcelItemStorageGUI(PluginConfiguration config) {
        this.config = config;
    }

    void show(Player player, ParcelSize size) {
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
            for (ItemStack item : this.gui.getInventory().getContents()) {
                player.getInventory().addItem(item);
                gui.removeItem(item);
            }
        });
        this.gui.open(player);

    }
}
