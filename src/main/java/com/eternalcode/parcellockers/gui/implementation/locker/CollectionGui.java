package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class CollectionGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final GuiSettings guiSettings;
    private final Scheduler scheduler;
    private final GuiManager guiManager;
    private final MiniMessage miniMessage;

    public CollectionGui(
        GuiSettings guiSettings,
        Scheduler scheduler,
        GuiManager guiManager,
        MiniMessage miniMessage
    ) {
        this.guiSettings = guiSettings;
        this.scheduler = scheduler;
        this.guiManager = guiManager;
        this.miniMessage = miniMessage;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        Component guiTitle = this.miniMessage.deserialize(this.guiSettings.parcelCollectionGuiTitle);
        ConfigItem parcelItem = this.guiSettings.parcelCollectionItem;

        PaginatedGui gui = Gui.paginated()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        this.setupStaticItems(player, gui);

        this.guiManager.getParcelsByReceiver(player.getUniqueId(), page).thenAccept(result -> {
            if (result == null || result.items().isEmpty()) {
                gui.setItem(22, this.guiSettings.noParcelsItem.toGuiItem());
                this.scheduler.run(() -> gui.open(player));
                return;
            }

            PaginatedGuiRefresher refresher = new PaginatedGuiRefresher(gui);

            this.setupNavigation(gui, page, result, player, this.guiSettings);


            AtomicInteger parcelCounter = new AtomicInteger(0);

            for (Parcel parcel : result.items()) {
                if (parcel.status() != ParcelStatus.DELIVERED) {
                    continue;
                }
                // TODO fix atomicinteger not changing with removals (refresher indexes changes, but counter not)
                final int parcelIndex = parcelCounter.getAndIncrement();

                // Stwórz supplier dla tego itemu
                Supplier<GuiItem> itemSupplier = () -> {
                    ConfigItem item = parcelItem.clone();
                    item.name(item.name().replace("{NAME}", parcel.name()));
                    item.lore(PlaceholderUtil.replaceParcelPlaceholders(parcel, item.lore(), this.guiManager));
                    item.glow(true);

                    return item.toGuiItem(event -> {
                        this.guiManager.collectParcel(player, parcel);
                        refresher.removeItemAt(parcelIndex);
                    });
                };

                refresher.addItem(itemSupplier);
                refresher.refresh();
            }

            this.scheduler.run(() -> gui.open(player));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    private void setupStaticItems(Player player, PaginatedGui gui) {
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();

        for (int cornerSlot : CORNER_SLOTS) {
            gui.setItem(cornerSlot, cornerItem);
        }

        for (int borderSlot : BORDER_SLOTS) {
            gui.setItem(borderSlot, backgroundItem);
        }

        gui.setItem(49, closeItem);
    }
}
