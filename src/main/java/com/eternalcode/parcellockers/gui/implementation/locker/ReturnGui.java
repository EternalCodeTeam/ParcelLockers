package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.util.DurationUtil;
import com.eternalcode.parcellockers.util.MaterialUtil;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ReturnGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final GuiSettings guiSettings;
    private final Scheduler scheduler;
    private final GuiManager guiManager;
    private final MiniMessage miniMessage;
    private final Function<Component, PaginatedGui> guiFactory;

    public ReturnGui(
        GuiSettings guiSettings,
        Scheduler scheduler,
        GuiManager guiManager,
        MiniMessage miniMessage
    ) {
        this(
            guiSettings,
            scheduler,
            guiManager,
            miniMessage,
            title -> Gui.paginated()
                .rows(6)
                .disableAllInteractions()
                .title(title)
                .create()
        );
    }

    ReturnGui(
        GuiSettings guiSettings,
        Scheduler scheduler,
        GuiManager guiManager,
        MiniMessage miniMessage,
        Function<Component, PaginatedGui> guiFactory
    ) {
        this.guiSettings = guiSettings;
        this.scheduler = scheduler;
        this.guiManager = guiManager;
        this.miniMessage = miniMessage;
        this.guiFactory = guiFactory;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        Component guiTitle = this.miniMessage.deserialize(this.guiSettings.parcelReturnGuiTitle);
        ConfigItem rowItem = this.guiSettings.parcelReturnRowItem;

        PaginatedGui gui = this.guiFactory.apply(guiTitle);

        this.setupStaticItems(player, gui);

        this.guiManager.getReturnableParcels(player.getUniqueId(), page).thenAccept(result -> {
            if (result.items().isEmpty()) {
                this.scheduler.run(() -> {
                    gui.setItem(22, this.guiSettings.noReturnableParcelsItem.toGuiItem());
                    gui.open(player);
                });
                return;
            }

            result.items().stream()
                .map(parcel -> this.createParcelItemAsync(parcel, rowItem, player))
                .collect(CompletableFutures.joinList())
                .thenAccept(suppliers -> this.scheduler.run(() -> {
                    if (suppliers.isEmpty()) {
                        gui.setItem(22, this.guiSettings.noReturnableParcelsItem.toGuiItem());
                        gui.open(player);
                        return;
                    }

                    this.setupNavigation(gui, page, result, player, this.guiSettings);
                    PaginatedGuiRefresher refresher = new PaginatedGuiRefresher(gui);
                    suppliers.forEach(refresher::addItem);
                    gui.open(player);
                }))
                .exceptionally(FutureHandler::handleException);
        }).exceptionally(FutureHandler::handleException);
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

    private CompletableFuture<Supplier<GuiItem>> createParcelItemAsync(
        Parcel parcel,
        ConfigItem rowItem,
        Player player
    ) {
        CompletableFuture<List<String>> loreFuture =
            PlaceholderUtil.replaceParcelPlaceholdersAsync(parcel, rowItem.lore(), this.guiManager);
        CompletableFuture<List<ItemStack>> contentFuture = this.guiManager.getParcelContent(parcel.uuid())
            .thenApply(optional -> optional.map(content -> content.items()).orElse(List.of()));
        CompletableFuture<String> windowLineFuture = this.guiManager.getCollectedInfo(parcel.uuid())
            .thenApply(optional -> optional
                .map(collected -> {
                    Duration remaining = Duration.between(Instant.now(), collected.collectedAt().plus(this.guiManager.returnWindow()));
                    return remaining.isNegative() || remaining.isZero()
                        ? this.guiSettings.returnWindowExpiredLine
                        : this.guiSettings.returnWindowRemainingLine.replace("{DURATION}", DurationUtil.format(remaining));
                })
                .orElse(this.guiSettings.returnWindowExpiredLine));

        return CompletableFutures.combine(loreFuture, contentFuture, windowLineFuture, (processedLore, items, windowLine) -> {
            Supplier<GuiItem> supplier = () -> {
                ConfigItem item = rowItem.clone();
                item.name(item.name().replace("{NAME}", parcel.name()));

                List<String> lore = new ArrayList<>(processedLore);
                lore.add(windowLine);
                if (!items.isEmpty()) {
                    lore.add(this.guiSettings.parcelItemsCollectionGui);
                    for (ItemStack itemStack : items) {
                        lore.add(this.guiSettings.parcelItemCollectionFormat
                            .replace("{ITEM}", MaterialUtil.format(itemStack.getType()))
                            .replace("{AMOUNT}", Integer.toString(itemStack.getAmount()))
                        );
                    }
                }

                item.lore(lore);
                item.glow(true);

                return item.toGuiItem(event -> new ReturnDepositGui(
                    this.scheduler,
                    this.guiSettings,
                    this.miniMessage,
                    this.guiManager,
                    parcel
                ).show(player));
            };
            return supplier;
        }).toCompletableFuture();
    }
}
