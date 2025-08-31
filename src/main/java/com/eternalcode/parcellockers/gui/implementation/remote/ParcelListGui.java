package com.eternalcode.parcellockers.gui.implementation.remote;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class ParcelListGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);
    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final GuiManager guiManager;
    private final MainGui mainGUI;

    public ParcelListGui(
        Scheduler scheduler,
        MiniMessage miniMessage,
        GuiSettings guiSettings,
        GuiManager guiManager,
        MainGui mainGUI
    ) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.guiManager = guiManager;
        this.mainGUI = mainGUI;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(resetItalic(this.miniMessage.deserialize(this.guiSettings.parcelListGuiTitle)))
            .rows(6)
            .disableAllInteractions()
            .create();

        this.setupStaticItems(player, gui);

        this.guiManager.getParcelsByReceiver(player.getUniqueId(), page).thenAccept(result -> {
            if (result.items().isEmpty() && page.hasPrevious()) {
                this.show(player, page.previous());
                return;
            }

            ConfigItem item = this.guiSettings.parcelItem;

            List<CompletableFuture<GuiItem>> itemFutures = result.items().stream()
                .map(parcel -> PlaceholderUtil.replaceParcelPlaceholdersAsync(parcel, item.lore(), this.guiManager)
                    .thenApply(processedLore -> {
                        PaperItemBuilder parcelItem = item.toBuilder();

                        List<Component> newLore = processedLore.stream()
                            .map(line -> resetItalic(this.miniMessage.deserialize(line)))
                            .toList();

                        parcelItem.lore(newLore);
                        parcelItem.name(this.miniMessage.deserialize(item.name().replace("{NAME}", parcel.name())));

                        return parcelItem.asGuiItem();
                    }))
                .toList();

            CompletableFutures.allAsList(itemFutures)
                .thenAccept(guiItems -> {
                    guiItems.forEach(gui::addItem);
                    this.setupNavigation(gui, page, result, player, this.guiSettings);
                    this.scheduler.run(() -> gui.open(player));
                })
                .exceptionally(throwable -> {
                    System.err.println("Failed to process parcel items: " + throwable.getMessage());
                    this.setupNavigation(gui, page, result, player, this.guiSettings);
                    this.scheduler.run(() -> gui.open(player));
                    return null;
                });
        });
    }

    private void setupStaticItems(Player player, PaginatedGui gui) {
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);
    }
}
