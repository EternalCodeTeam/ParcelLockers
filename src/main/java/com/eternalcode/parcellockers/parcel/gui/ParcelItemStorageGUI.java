package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
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
import java.util.stream.IntStream;

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
        
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));

        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(event -> {
            this.confirmed = true;
            new ParcelSendingGUI(this.config, this.miniMessage, itemStorageRepository).show(player);
        });
        GuiItem cancelItem = guiSettings.cancelItemsItem.toGuiItem(event -> {
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

        IntStream.rangeClosed(3, 9).forEach(i -> this.gui.setItem(this.gui.getRows(), i, backgroundItem));

        this.gui.setItem(this.gui.getRows(), 1, confirmItem);
        this.gui.setItem(this.gui.getRows(), 2, cancelItem);

        this.gui.setCloseGuiAction(event -> {
            ItemStack[] contents = this.gui.getInventory().getContents();

            if (this.confirmed) {
                List<String> serialized = new ArrayList<>();

                for (int i = 0; i < contents.length - 9; i++) {
                    ItemStack item = contents[i];

                    if (item == null) {
                        continue;
                    }

                    serialized.add(ItemUtil.itemStackToString(item));
                }

                this.itemStorageRepository.save(new ItemStorage(player.getUniqueId(), serialized));
                return;
            }

            for (int i = 0; i < contents.length - 9; i++) {
                ItemStack item = contents[i];
                if (item == null) {
                    continue;
                }

                player.getInventory().addItem(item); // TODO: zrobić metodę (InventoryUtil) która będzie dodawać itemy do inventory gracza i jeśli nie będzie mieć miejsca to wywalić itemy na ziemię
                this.gui.removeItem(item);
            }
        });
        
        this.gui.open(player);
    }
}
