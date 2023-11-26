package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.util.InventoryUtil;
import com.eternalcode.parcellockers.util.ItemUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ParcelItemStorageGUI {

    private final Plugin plugin;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;

    private boolean confirmed;

    public ParcelItemStorageGUI(Plugin plugin, PluginConfiguration config, MiniMessage miniMessage, ItemStorageRepository itemStorageRepository) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
    }

    void show(Player player, ParcelSize size) {
        StorageGui gui;
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;
        
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));

        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(event -> {
            this.confirmed = true;
            new ParcelSendingGUI(plugin, this.config, this.miniMessage, itemStorageRepository).show(player);
        });
        GuiItem cancelItem = guiSettings.cancelItemsItem.toGuiItem(event -> {
            this.confirmed = false;
            new ParcelSendingGUI(plugin, this.config, this.miniMessage, itemStorageRepository).show(player);
        });

        switch (size) {
            case SMALL -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelSmallContentGuiTitle))
                .rows(2)
                .create();
            case MEDIUM -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelMediumContentGuiTitle))
                .rows(3)
                .create();
            case LARGE -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(guiSettings.parcelLargeContentGuiTitle))
                .rows(4)
                .create();
            default -> throw new IllegalStateException("Unexpected value: " + size);
        }

        // Set background items and confirm/cancel items + close gui action
        IntStream.rangeClosed(3, 9).forEach(i -> gui.setItem(gui.getRows(), i, backgroundItem));
        gui.setItem(gui.getRows(), 1, confirmItem);
        gui.setItem(gui.getRows(), 2, cancelItem);

        gui.setCloseGuiAction(event -> {
            ItemStack[] contents = gui.getInventory().getContents();

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

                InventoryUtil.addItem(player, item);
                gui.removeItem(item);
            }
        }); // TODO: fix serialization

        this.itemStorageRepository.find(player.getUniqueId()).whenComplete((optional, throwable) -> {
            if (optional.isEmpty()) {
                return;
            }

            ItemStorage itemStorage = optional.get();

            for (String serializedItemStack : itemStorage.serializedItemStacks()) {
                ItemStack itemStack = ItemUtil.stringToItemStack(serializedItemStack);

                gui.addItem(itemStack);
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> gui.open(player));
        });
    }
}
