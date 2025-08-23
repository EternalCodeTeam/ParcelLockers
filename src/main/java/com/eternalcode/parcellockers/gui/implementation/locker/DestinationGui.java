package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings("ClassCanBeRecord")
public class DestinationGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final SendingGui sendingGUI;
    private final SendingGuiState state;

    public DestinationGui(
        Scheduler scheduler,
        GuiSettings guiSettings,
        MiniMessage miniMessage,
        GuiManager guiManager,
        SendingGui sendingGUI,
        SendingGuiState state
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.sendingGUI = sendingGUI;
        this.state = state;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        GuiItem previousPageItem = this.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));

        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.guiSettings.parcelDestinationLockerSelectionGuiTitle))
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

        this.guiManager.getLockerPage(page).thenAccept(result -> {
                if (result.hasNextPage()) {
                    gui.setItem(51, nextPageItem);
                }

                if (page.hasPrevious()) {
                    gui.setItem(47, previousPageItem);
                }

                this.loadLockers(player, result, refresher).forEach(refresher::addItem);
                this.scheduler.run(() -> gui.open(player));
            }).orTimeout(5, TimeUnit.SECONDS);
    }

    private List<Supplier<GuiItem>> loadLockers(Player player, PageResult<Locker> result, PaginatedGuiRefresher refresh) {
        return result.items().stream()
            .map(locker -> this.toItem(player, locker, refresh))
            .toList();
    }

    private Supplier<GuiItem> toItem(Player player, Locker locker, PaginatedGuiRefresher refresher) {
        UUID uuid = locker.uuid();
        ConfigItem parcelItem = this.guiSettings.destinationLockerItem.clone();

        return () -> {
            String name = parcelItem.name().replace("{DESCRIPTION}", locker.description());
            boolean isLockerSelected = uuid.equals(this.state.destinationLocker());
            String oneLineLore = isLockerSelected
                ? this.guiSettings.parcelDestinationSetLine
                : this.guiSettings.parcelDestinationNotSetLine;

            return parcelItem
                .name(name)
                .glow(isLockerSelected)
                .lore(List.of(oneLineLore))
                .toGuiItem(event -> {
                    if (isLockerSelected) {
                        this.state.destinationLocker(null);
                        this.sendingGUI.updateDestinationItem(player, "");
                        refresher.refresh();
                        return;
                    }

                    this.state.destinationLocker(uuid);
                    this.sendingGUI.updateDestinationItem(player, locker.description());
                    refresher.refresh();
                });
        };
    }
}
