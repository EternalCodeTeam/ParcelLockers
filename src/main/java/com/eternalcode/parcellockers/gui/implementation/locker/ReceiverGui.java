package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.repository.UserPageResult;
import com.spotify.futures.CompletableFutures;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.rollczi.liteskullapi.SkullData;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings("ClassCanBeRecord")
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

    private void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.guiSettings.parcelReceiverSelectionGuiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));
        GuiItem previousPageItem = this.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);

        this.guiManager.getUserPage(page).thenAccept(result -> {
            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            PaginatedGuiRefresher refresh = new PaginatedGuiRefresher(gui);

            this.loadSkulls(player, result, refresh).thenAccept(items -> {
                for (Supplier<GuiItem> item : items) {
                    refresh.addItem(item);
                }

                this.scheduler.run(() -> gui.open(player));
            });
        });
    }

    private CompletableFuture<List<Supplier<GuiItem>>> loadSkulls(Player player, UserPageResult result, PaginatedGuiRefresher refresh) {
        return result.users().stream()
//            .filter(user -> !user.uuid().equals(player.getUniqueId()))
            .map(user -> this.skullAPI.getSkullData(user.uuid()).thenApply(skullData -> this.toItem(player, user, skullData, refresh)))
            .collect(CompletableFutures.joinList());
    }

    private Supplier<GuiItem> toItem(Player player, User user, SkullData skullData, PaginatedGuiRefresher refresher) {
        UUID uuid = user.uuid();

        return () -> {
            boolean isReceiverSelected = uuid.equals(this.state.receiver());
            String lore = isReceiverSelected
                ? this.guiSettings.parcelReceiverSetLine
                : this.guiSettings.parcelReceiverNotSetLine;

            return ItemBuilder.skull()
                .texture(skullData.getTexture())
                .name(this.miniMessage.deserialize(user.name()))
                .lore(this.miniMessage.deserialize(lore))
                .glow(uuid.equals(this.state.receiver()))
                .asGuiItem(event -> {
                    if (uuid.equals(this.state.receiver())) {
                        refresher.refresh();
                        this.sendingGUI.updateReceiverItem(player, "");
                        this.state.receiver(null);
                        return;
                    }

                    this.sendingGUI.updateReceiverItem(player, user.name());
                    this.state.receiver(uuid);
                    refresher.refresh();
                });
        };
    }
}
