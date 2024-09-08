package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerPageResult;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.shared.ExceptionHandler;
import com.eternalcode.parcellockers.shared.Page;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DestinationSelectionGUI extends GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final LockerRepository lockerRepository;
    private final ParcelSendingGUI sendingGUI;
    private final ParcelSendingGUIState state;

    public DestinationSelectionGUI(Plugin plugin, BukkitScheduler scheduler, PluginConfiguration config, MiniMessage miniMessage, LockerRepository lockerRepository, ParcelSendingGUI sendingGUI, ParcelSendingGUIState state) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.config = config;
        this.miniMessage = miniMessage;
        this.lockerRepository = lockerRepository;
        this.sendingGUI = sendingGUI;
        this.state = state;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        GuiItem previousPageItem = this.config.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.config.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));

        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.config.guiSettings.parcelDestinationLockerSelectionGuiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        PaginatedGuiRefresher refresher = new PaginatedGuiRefresher(gui);

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);

        this.lockerRepository.findPage(page).thenAccept(result -> {
            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            this.loadLockers(player, result, refresher).forEach(refresher::addItem);
            this.scheduler.runTask(this.plugin, () -> gui.open(player));
        }).whenComplete(ExceptionHandler.handler())
            .orTimeout(5, TimeUnit.SECONDS);
    }

    private List<Supplier<GuiItem>> loadLockers(Player player, LockerPageResult result, PaginatedGuiRefresher refresh) {
        return result.lockers().stream()
            .map(locker -> this.toItem(player, locker, refresh))
            .toList();
    }

    private Supplier<GuiItem> toItem(Player player, Locker locker, PaginatedGuiRefresher refresher) {
        UUID uuid = locker.uuid();
        ConfigItem parcelItem = this.config.guiSettings.destinationLockerItem.clone();

        return () -> {
            String name = parcelItem.getName().replace("{DESCRIPTION}", locker.description());
            boolean isLockerSelected = uuid.equals(this.state.getDestinationLocker());
            String oneLineLore = isLockerSelected
                ? this.config.guiSettings.parcelDestinationSetLine
                : this.config.guiSettings.parcelDestinationNotSetLine;

            return parcelItem
                .setName(name)
                .setGlow(isLockerSelected)
                .setLore(List.of(oneLineLore))
                .toGuiItem(event -> {
                    if (isLockerSelected) {
                        this.state.setDestinationLocker(null);
                        this.sendingGUI.updateDestinationItem(player, null, "");
                        refresher.refresh();
                        return;
                    }

                    this.state.setDestinationLocker(uuid);
                    this.sendingGUI.updateDestinationItem(player, uuid, locker.description());
                    refresher.refresh();
                });
        };
    }
}
