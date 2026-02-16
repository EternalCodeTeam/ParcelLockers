package com.eternalcode.parcellockers.gui.implementation.locker;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.spotify.futures.CompletableFutures;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.rollczi.liteskullapi.SkullData;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class ReceiverGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Scheduler scheduler;
    private final GuiSettings guiSettings;
    private final MiniMessage miniMessage;
    private final GuiManager guiManager;
    private final SendingGui sendingGUI;
    private final SkullAPI skullAPI;
    private final SendingGuiState state;

    public ReceiverGui(
            Scheduler scheduler,
            GuiSettings guiSettings,
            MiniMessage miniMessage,
            GuiManager guiManager,
            SendingGui sendingGUI,
            SkullAPI skullAPI,
            SendingGuiState state
    ) {
        this.scheduler = scheduler;
        this.guiSettings = guiSettings;
        this.miniMessage = miniMessage;
        this.guiManager = guiManager;
        this.sendingGUI = sendingGUI;
        this.skullAPI = skullAPI;
        this.state = state;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    @Override
    public void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.guiSettings.parcelReceiverSelectionGuiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        this.setupStaticItems(player, gui);

        this.guiManager.getUsers(page).thenAccept(result -> {
            this.setupNavigation(gui, page, result, player);

            PaginatedGuiRefresher refresh = new PaginatedGuiRefresher(gui);

            this.loadSkulls(player, result, refresh).thenAccept(items -> {
                for (Supplier<GuiItem> item : items) {
                    refresh.addItem(item);
                }

                this.scheduler.run(() -> gui.open(player));
            });
        });
    }

    private CompletableFuture<List<Supplier<GuiItem>>> loadSkulls(Player player, PageResult<User> result, PaginatedGuiRefresher refresh) {
        return result.items().stream()
            //            .filter(user -> !user.uuid().equals(player.getUniqueId()))
            .map(user -> this.skullAPI.getSkullData(user.uuid())
                .thenApply(skullData -> this.toItem(player, user, skullData, refresh))
                .exceptionally(FutureHandler::handleException))
            .collect(CompletableFutures.joinList())
            .orTimeout(10, TimeUnit.SECONDS);
    }

    private Supplier<GuiItem> toItem(Player player, User user, SkullData skullData, PaginatedGuiRefresher refresher) {
        UUID uuid = user.uuid();

        return () -> {
            boolean isReceiverSelected = uuid.equals(this.state.receiver());
            String lore = isReceiverSelected
                ? this.guiSettings.parcelReceiverSetLine
                : this.guiSettings.parcelReceiverNotSetLine;

            return PaperItemBuilder.skull()
                .texture(skullData.getTexture())
                .name(resetItalic(this.miniMessage.deserialize(user.name())))
                .lore(resetItalic(this.miniMessage.deserialize(lore)))
                .glow(isReceiverSelected)
                .asGuiItem(event -> {
                    if (isReceiverSelected) {
                        this.sendingGUI.updateReceiverItem(player, "", false);
                        this.state.receiver(null);
                        refresher.refresh();
                        return;
                    }

                    this.sendingGUI.updateReceiverItem(player, user.name(), true);
                    this.state.receiver(uuid);
                    refresher.refresh();
                });
        };
    }

    private void setupStaticItems(Player player, PaginatedGui gui) {
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }
        gui.setItem(49, closeItem);
    }

    private void setupNavigation(PaginatedGui gui, Page page, PageResult<User> result, Player player) {
        if (result.hasNextPage()) {
            GuiItem nextPageItem = this.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));
            gui.setItem(51, nextPageItem);
        }

        if (page.hasPrevious()) {
            GuiItem previousPageItem = this.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
            gui.setItem(47, previousPageItem);
        }
    }
}
