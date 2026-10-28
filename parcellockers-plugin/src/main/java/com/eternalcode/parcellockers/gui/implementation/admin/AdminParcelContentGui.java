package com.eternalcode.parcellockers.gui.implementation.admin;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.util.MaterialUtil;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AdminParcelContentGui {

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final NoticeService noticeService;
    private final Parcel parcel;
    private final Consumer<Player> onClose;

    public AdminParcelContentGui(Scheduler scheduler, GuiSettings guiSettings, MiniMessage miniMessage,
            GuiManager guiManager, NoticeService noticeService, Parcel parcel, Consumer<Player> onClose) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.noticeService = noticeService;
        this.parcel = parcel;
        this.onClose = onClose;
    }

    public void show(Player player) {
        int rows = switch (this.parcel.size()) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 4;
        };

        StorageGui gui = dev.triumphteam.gui.guis.Gui.storage()
            .title(this.miniMessage.deserialize(this.guiSettings.adminParcelContentGuiTitle))
            .rows(rows)
            .create();

        GuiItem background = this.guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));
        IntStream.rangeClosed(1, 9).forEach(i -> gui.setItem(gui.getRows(), i, background));
        gui.setItem(gui.getRows(), 5, this.guiSettings.confirmItemsItem.toGuiItem(event -> {
            event.setCancelled(true);
            gui.close(player);
        }));

        gui.setCloseGuiAction(event -> {
            ItemStack[] contents = gui.getInventory().getContents();
            List<ItemStack> items = new ArrayList<>();
            List<ItemStack> illegalItems = new ArrayList<>();
            for (int i = 0; i < contents.length - 9; i++) {
                ItemStack item = contents[i];
                if (item == null) {
                    continue;
                }
                if (this.guiSettings.illegalItems.contains(item.getType())) {
                    illegalItems.add(item);
                } else {
                    items.add(item);
                }
            }
            for (ItemStack illegalItem : illegalItems) {
                ItemUtil.giveItem(player, illegalItem);
                this.noticeService.create()
                    .notice(messages -> messages.parcel.illegalItem)
                    .placeholder("{ITEMS}", MaterialUtil.format(illegalItem.getType()))
                    .player(player.getUniqueId())
                    .send();
            }
            this.guiManager.updateParcelContent(this.parcel.uuid(), items)
                .thenAccept(saved -> {
                    this.noticeService.create().notice(m -> m.admin.contentsUpdated).player(player.getUniqueId()).send();
                    this.scheduler.run(() -> this.onClose.accept(player));
                })
                .exceptionally(throwable -> {
                    this.scheduler.run(() -> items.forEach(item -> ItemUtil.giveItem(player, item)));
                    return FutureHandler.handleException(throwable);
                });
        });

        this.guiManager.getParcelContent(this.parcel.uuid()).thenAccept(optional -> {
            optional.ifPresent(content -> gui.addItem(content.items().toArray(new ItemStack[0])));
            this.scheduler.run(() -> gui.open(player));
        }).exceptionally(FutureHandler::handleException);
    }
}
