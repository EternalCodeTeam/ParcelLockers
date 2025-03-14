package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import com.eternalcode.parcellockers.util.InventoryUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class ParcelCollectionGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Plugin plugin;
    private final PluginConfiguration config;
    private final BukkitScheduler scheduler;
    private final ParcelRepository parcelRepository;
    private final MiniMessage miniMessage;
    private final ParcelManager parcelManager;

    public ParcelCollectionGui(Plugin plugin, PluginConfiguration config, BukkitScheduler scheduler, ParcelRepository parcelRepository, MiniMessage miniMessage, ParcelManager parcelManager) {
        this.plugin = plugin;
        this.config = config;
        this.scheduler = scheduler;
        this.parcelRepository = parcelRepository;
        this.miniMessage = miniMessage;
        this.parcelManager = parcelManager;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        PluginConfiguration.GuiSettings guiSettings = this.config.guiSettings;

        Component guiTitle = this.miniMessage.deserialize(guiSettings.parcelCollectionGuiTitle);

        PaginatedGui gui = Gui.paginated()
            .rows(6)
            .disableAllInteractions()
            .title(guiTitle)
            .create();

        ConfigItem parcelItem = guiSettings.parcelCollectionItem;
        GuiItem closeItem = guiSettings.closeItem.toGuiItem(event -> gui.close(player));
        GuiItem cornerItem = guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem nextPageItem = guiSettings.nextPageItem.toGuiItem(event -> {
            Page nextPage = new Page(page.page() + 1, page.size());
            this.show(player, nextPage);
        });

        GuiItem previousPageItem = guiSettings.previousPageItem.toGuiItem(event -> {
            Page previousPage = new Page(page.page() - 1, page.size());
            this.show(player, previousPage);
        });

        for (int cornerSlot : CORNER_SLOTS) {
            gui.setItem(cornerSlot, cornerItem);
        }

        for (int borderSlot : BORDER_SLOTS) {
            gui.setItem(borderSlot, backgroundItem);
        }

        gui.setItem(49, closeItem);

        this.parcelRepository.findByReceiver(player.getUniqueId(), page).thenAccept(result -> {
            if (result == null || result.parcels().isEmpty()) {
                gui.setItem(22, guiSettings.noParcelsItem.toGuiItem(event -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT, 0.5F, 1)));
            }

            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            for (Parcel parcel : result.parcels()) {
                ConfigItem item = parcelItem.clone();
                item.name = item.name.replace("{NAME}", parcel.name());
                item.lore = item.lore.stream()
                    .map(line -> line.replace("{UUID}", parcel.uuid().toString()))
                    .map(line -> line.replace("{DESCRIPTION}", parcel.description() == null ? "" : parcel.description()))
                    .map(line -> line.replace("{SIZE}", parcel.size().name()))
                    .map(line -> line.replace("{SENDER}", parcel.sender().toString()))
                    .toList();

                item.setGlow(true);

                gui.addItem(item.toGuiItem(event -> {
                    this.parcelManager.collectParcel(player, parcel);
                    gui.removeItem(event.getSlot());
                    InventoryUtil.shiftItems(event.getSlot(), gui, item.type);
                    gui.update();
                }));
            }

            this.scheduler.runTask(this.plugin, () -> gui.open(player));
        }).whenComplete(SentryExceptionHandler.handler());
    }
}
