package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.util.MaterialUtil;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelItemStorageGui {

    private final Scheduler scheduler;
    private final PluginConfig config;
    private final MiniMessage miniMessage;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final NoticeService noticeService;
    private final ParcelContentRepository parcelContentRepository;
    private final UserRepository userRepository;
    private final SkullAPI skullAPI;
    private final ParcelSendingGuiState state;
    private final ParcelService parcelService;

    public ParcelItemStorageGui(
        Scheduler scheduler,
        PluginConfig config,
        MiniMessage miniMessage,
        ItemStorageRepository itemStorageRepository,
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        NoticeService noticeService,
        ParcelContentRepository parcelContentRepository,
        UserRepository userRepository,
        SkullAPI skullAPI,
        ParcelSendingGuiState state, ParcelService parcelService
    ) {
        this.scheduler = scheduler;
        this.config = config;
        this.miniMessage = miniMessage;
        this.itemStorageRepository = itemStorageRepository;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.noticeService = noticeService;
        this.parcelContentRepository = parcelContentRepository;
        this.userRepository = userRepository;
        this.skullAPI = skullAPI;
        this.state = state;
        this.parcelService = parcelService;
    }

    void show(Player player, ParcelSize size) {
        StorageGui gui;
        GuiSettings guiSettings = this.config.guiSettings;

        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));

        GuiItem confirmItem = guiSettings.confirmItemsItem.toGuiItem(event -> new ParcelSendingGui(
            this.scheduler,
            this.config,
            this.miniMessage,
            this.itemStorageRepository,
            this.parcelRepository,
            this.lockerRepository,
            this.noticeService,
            this.parcelContentRepository,
            this.userRepository,
            this.skullAPI,
            this.parcelService,
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
        IntStream.rangeClosed(1, 9).forEach(i -> gui.setItem(gui.getRows(), i, backgroundItem));
        gui.setItem(gui.getRows(), 5, confirmItem);

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
                        ItemUtil.giveItem(player, item);
                        gui.removeItem(item);
                        this.noticeService.create()
                            .notice(messages -> messages.parcel.illegalItem)
                            .placeholder("{ITEMS}", MaterialUtil.format(item.getType()))
                            .player(player.getUniqueId())
                            .send();
                    }
                }

                items.add(item);
            }

            this.itemStorageRepository.delete(player.getUniqueId()).thenAccept(unused -> {
                this.itemStorageRepository.save(new ItemStorage(player.getUniqueId(), items));
            });
        });

        this.itemStorageRepository.find(player.getUniqueId()).thenAccept(optional -> {
            if (optional.isPresent()) {
                ItemStorage itemStorage = optional.get();

                for (ItemStack item : itemStorage.items()) {
                    gui.addItem(item);
                }
            }

            this.scheduler.run(() -> gui.open(player));
        });
    }
}
