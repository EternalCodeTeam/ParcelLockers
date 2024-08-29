package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.ExceptionHandler;
import com.eternalcode.parcellockers.user.UserRepository;
import com.eternalcode.parcellockers.util.InventoryUtil;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final ParcelContentRepository parcelContentRepository;
    private final UserRepository userRepository;
    private final SkullAPI skullAPI;
    private final ParcelSendingGUIState state;

    public ParcelItemStorageGUI(Plugin plugin,
                                PluginConfiguration config,
                                MiniMessage miniMessage,
                                ItemStorageRepository itemStorageRepository,
                                ParcelRepository parcelRepository,
                                LockerRepository lockerRepository,
                                NotificationAnnouncer announcer,
                                ParcelContentRepository parcelContentRepository,
                                UserRepository userRepository,
                                SkullAPI skullAPI,
                                ParcelSendingGUIState state)
    {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.parcelContentRepository = parcelContentRepository;
        this.userRepository = userRepository;
        this.skullAPI = skullAPI;
        this.state = state;
    }

    void show(Player player, ParcelSize size) {
        StorageGui gui;
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));

        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(event -> new ParcelSendingGUI(
            this.plugin,
            this.config,
            this.miniMessage,
            this.itemStorageRepository,
            this.parcelRepository,
            this.lockerRepository,
            this.announcer,
            this.parcelContentRepository,
            this.userRepository,
            this.skullAPI,
            this.state
            ).show(player));

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
        IntStream.rangeClosed(2, 9).forEach(i -> gui.setItem(gui.getRows(), i, backgroundItem));
        gui.setItem(gui.getRows(), 1, confirmItem);

        gui.setCloseGuiAction(event -> {
            ItemStack[] contents = gui.getInventory().getContents();

            List<ItemStack> items = new ArrayList<>();

            for (int i = 0; i < contents.length - 9; i++) {
                ItemStack item = contents[i];

                if (item == null) {
                    continue;
                }

                for (Material type : this.config.guiSettings.illegalItems) {
                    if (item.getType() == type) {
                        InventoryUtil.addItem(player, item);
                        gui.removeItem(item);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1, 1);
                        player.sendMessage(ChatColor.RED + "This item is illegal and cannot be send!");
                    }
                }

                items.add(item);
            }

            this.itemStorageRepository.remove(player.getUniqueId()).thenAccept(unused -> {
                this.itemStorageRepository.save(new ItemStorage(player.getUniqueId(), items));
            }).whenComplete(ExceptionHandler.handler());
        });

        this.itemStorageRepository.find(player.getUniqueId()).thenAccept(optional -> {
            if (optional.isPresent()) {
                ItemStorage itemStorage = optional.get();

                for (ItemStack item : itemStorage.items()) {
                    gui.addItem(item);
                }
            }

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> gui.open(player));
        }).whenComplete(ExceptionHandler.handler());
    }
}
