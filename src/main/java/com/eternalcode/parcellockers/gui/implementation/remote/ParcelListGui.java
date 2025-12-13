package com.eternalcode.parcellockers.gui.implementation.remote;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.util.DurationUtil;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class ParcelListGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

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

            if (result.items().isEmpty()) {
                gui.setItem(22, this.guiSettings.noParcelsItem.toGuiItem());
                this.scheduler.run(() -> gui.open(player));
                return;
            }

            ConfigItem item = this.guiSettings.parcelItem;

            List<CompletableFuture<GuiItem>> itemFutures = result.items().stream()
                .map(parcel -> this.createParcelItemAsync(parcel, item))
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

    private CompletableFuture<GuiItem> createParcelItemAsync(Parcel parcel, ConfigItem item) {
        CompletableFuture<List<String>> loreFuture = PlaceholderUtil.replaceParcelPlaceholdersAsync(
            parcel, item.lore(), this.guiManager
        );
        CompletableFuture<Optional<Delivery>> deliveryFuture = this.guiManager.getDelivery(parcel.uuid());

        return loreFuture.thenCombine(deliveryFuture, (processedLore, deliveryOptional) -> {
            PaperItemBuilder parcelItem = item.toBuilder();

            List<String> loreWithArrival = new ArrayList<>(processedLore);

            if (deliveryOptional.isPresent()) {
                Delivery delivery = deliveryOptional.get();
                Instant arrivalTime = delivery.deliveryTimestamp();
                Instant now = Instant.now();

                if (arrivalTime.isAfter(now)) {
                    Duration remaining = Duration.between(now, arrivalTime);
                    String formattedDuration = DurationUtil.format(remaining);
                    String formattedDate = DATE_FORMATTER.format(arrivalTime.atZone(ZoneId.systemDefault()));

                    String arrivingLine = this.guiSettings.parcelArrivingLine
                        .replace("{DURATION}", formattedDuration)
                        .replace("{DATE}", formattedDate);

                    loreWithArrival.add(arrivingLine);
                } else if (arrivalTime.isBefore(now)) { // not supported rn, because deliveries are deleted on arrival, so the if is always false
                    String arrivedLine = this.guiSettings.parcelArrivedLine
                        .replace("{DATE}", DATE_FORMATTER.format(arrivalTime.atZone(ZoneId.systemDefault())));
                    loreWithArrival.add(arrivedLine);
                }
            }

            List<Component> newLore = loreWithArrival.stream()
                .map(line -> resetItalic(this.miniMessage.deserialize(line)))
                .toList();

            parcelItem.lore(newLore);
            parcelItem.name(resetItalic(this.miniMessage.deserialize(item.name().replace("{NAME}", parcel.name()))));

            return parcelItem.asGuiItem();
        });
    }

}
