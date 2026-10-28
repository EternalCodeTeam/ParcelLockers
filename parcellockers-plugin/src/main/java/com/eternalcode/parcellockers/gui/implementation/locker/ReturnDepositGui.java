package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Deposit GUI for a parcel return: the player places the original items, then confirms.
 * Confirm hands the stacks to the return service (which gives them back on any failure);
 * closing without confirming gives everything back immediately.
 */
public class ReturnDepositGui {

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final Parcel parcel;
    private final BiFunction<Component, Integer, StorageGui> guiFactory;

    public ReturnDepositGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        GuiManager guiManager,
        Parcel parcel
    ) {
        this(
            scheduler,
            guiSettings,
            miniMessage,
            guiManager,
            parcel,
            (title, rows) -> Gui.storage().title(title).rows(rows).create()
        );
    }

    ReturnDepositGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        GuiManager guiManager,
        Parcel parcel,
        BiFunction<Component, Integer, StorageGui> guiFactory
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.parcel = parcel;
        this.guiFactory = guiFactory;
    }

    void show(Player player) {
        int rows = switch (this.parcel.size()) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 4;
        };

        Component title = this.miniMessage.deserialize(this.guiSettings.parcelReturnDepositGuiTitle);
        StorageGui gui = this.guiFactory.apply(title, rows);

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem(event -> event.setCancelled(true));
        IntStream.rangeClosed(1, 9).forEach(i -> gui.setItem(gui.getRows(), i, backgroundItem));

        AtomicBoolean confirmed = new AtomicBoolean(false);

        GuiItem confirmItem = this.guiSettings.confirmReturnItem.toGuiItem(event -> {
            event.setCancelled(true);

            List<ItemStack> deposited = this.takeDepositedItems(gui);
            confirmed.set(true);
            gui.close(player);
            this.guiManager.returnParcel(player, this.parcel, deposited);
        });
        gui.setItem(gui.getRows(), 5, confirmItem);

        gui.setCloseGuiAction(event -> {
            if (confirmed.get()) {
                return;
            }
            List<ItemStack> leftovers = this.takeDepositedItems(gui);
            this.scheduler.run(() -> leftovers.forEach(item -> ItemUtil.giveItem(player, item)));
        });

        this.scheduler.run(() -> gui.open(player));
    }

    private List<ItemStack> takeDepositedItems(StorageGui gui) {
        ItemStack[] contents = gui.getInventory().getContents();
        List<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < contents.length - 9; i++) {
            ItemStack item = contents[i];
            if (item == null || item.isEmpty()) {
                continue;
            }
            items.add(item.clone());
            gui.getInventory().setItem(i, null);
        }
        return items;
    }
}
