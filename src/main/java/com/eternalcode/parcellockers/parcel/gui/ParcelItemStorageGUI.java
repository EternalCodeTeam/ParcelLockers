package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.feature.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.feature.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.util.ItemUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ParcelItemStorageGUI {

    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;

    private StorageGui gui;
    private boolean confirmed;

    public ParcelItemStorageGUI(PluginConfiguration config, MiniMessage miniMessage, ItemStorageRepository itemStorageRepository) {
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
    }

    void show(Player player, ParcelSize size) {
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;
        
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = true;
            new ParcelSendingGUI(this.config, this.miniMessage, itemStorageRepository).show(player);
        });
        GuiItem cancelItem = guiSettings.cancelItemsItem.toGuiItem(this.miniMessage, event -> {
            this.confirmed = false;
            new ParcelSendingGUI(this.config, this.miniMessage, itemStorageRepository).show(player);
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
                List<String> serialized = new ArrayList<>();
                for (ItemStack item : this.gui.getInventory().getContents()) {
                    serialized.add(ItemUtil.itemStackToString(item));
                }
                this.itemStorageRepository.save(new ItemStorage(player.getUniqueId(), serialized));
                return;
            }
            
            if (this.gui.getInventory().getContents() == null) {
                return;
            }
                
            for (ItemStack item : this.gui.getInventory().getContents()) {
                if (item == null || ItemUtil.compareMeta(confirmItem.getItemStack(), item) || ItemUtil.compareMeta(cancelItem.getItemStack(), item)) {
                    continue;
                }
                    
                player.getInventory().addItem(item);
                this.gui.removeItem(item);
            }
        });
        
        this.gui.open(player);
    }
}
