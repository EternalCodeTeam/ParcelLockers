package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.ParcelSize;
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

public class ItemStorageGui {

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final NoticeService noticeService;
    private final SkullAPI skullAPI;
    private final SendingGuiState state;

    public ItemStorageGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        GuiManager guiManager,
        NoticeService noticeService,
        SkullAPI skullAPI,
        SendingGuiState state
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.noticeService = noticeService;
        this.skullAPI = skullAPI;
        this.state = state;
    }

    void show(Player player, ParcelSize size) {
        StorageGui gui;

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));

        GuiItem confirmItem = this.guiSettings.confirmItemsItem.toGuiItem(event -> new SendingGui(
            this.scheduler,
            this.guiSettings,
            this.miniMessage,
            this.noticeService,
            this.guiManager,
            this.skullAPI,
            this.state
        ).show(player));

        switch (size) {
            case SMALL -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(this.guiSettings.parcelSmallContentGuiTitle))
                .rows(2)
                .create();
            case MEDIUM -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(this.guiSettings.parcelMediumContentGuiTitle))
                .rows(3)
                .create();
            case LARGE -> gui = Gui.storage()
                .title(this.miniMessage.deserialize(this.guiSettings.parcelLargeContentGuiTitle))
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

                for (Material type : this.guiSettings.illegalItems) {
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

            this.guiManager.deleteItemStorage(player.getUniqueId()).thenAccept(
                unused -> this.guiManager.saveItemStorage(new ItemStorage(player.getUniqueId(), items))
            );
        });

        this.guiManager.getItemStorage(player.getUniqueId()).thenAccept(optional -> {
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
