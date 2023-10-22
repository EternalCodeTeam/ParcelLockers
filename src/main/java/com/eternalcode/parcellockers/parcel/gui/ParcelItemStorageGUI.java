package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
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
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = true;
            new ParcelSendingGUI(this.config, this.miniMessage).show(player);
        });
        GuiItem cancelItem = guiSettings.cancelItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = false;
            new ParcelSendingGUI(this.config, this.miniMessage).show(player);
        });

        switch (size) {
            case SMALL -> this.gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelSmallContentGuiTitle))
                .rows(2)
                .create();
            case MEDIUM -> this.gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelMediumContentGuiTitle))
                .rows(3)
                .create();
            case LARGE -> this.gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelLargeContentGuiTitle))
                .rows(4)
                .create();
            default -> throw new IllegalStateException("Unexpected value: " + size);
        }

        for (int i = 0; i < 8; i++) {
            this.gui.setItem(this.gui.getRows(), i, backgroundItem);
        }

        this.gui.setItem(this.gui.getRows(), 1, confirmItem);
        this.gui.setItem(this.gui.getRows(), 2, cancelItem);
        this.gui.setCloseGuiAction(event -> {
            if (this.confirmed) {
                return;
            }

            if (this.gui.getInventory().getContents() == null) {
                return;
            }

            for (ItemStack item : this.gui.getInventory().getContents()) {
                if (item == null) {
                    continue;
                }

                player.getInventory().addItem(item);
                this.gui.removeItem(item);
            }
        });

        this.gui.open(player);
    }
}
