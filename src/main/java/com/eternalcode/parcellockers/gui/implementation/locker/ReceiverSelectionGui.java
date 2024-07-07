package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.gui.PaginatedGuiRefresher;
import com.eternalcode.parcellockers.shared.ExceptionHandler;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserPageResult;
import com.eternalcode.parcellockers.user.UserRepository;
import com.spotify.futures.CompletableFutures;
import dev.rollczi.liteskullapi.SkullAPI;
import dev.rollczi.liteskullapi.SkullData;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// TODO: Maybe in future cache this globally, so props wont change after gui exit, because new instance is created.

public class ReceiverSelectionGui extends GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final UserRepository userRepository;
    private final ParcelSendingGUI sendingGUI;
    private final SkullAPI skullAPI;

    private @Nullable UUID receiver;

    public ReceiverSelectionGui(Plugin plugin, BukkitScheduler scheduler, PluginConfiguration config, MiniMessage miniMessage, UserRepository userRepository, ParcelSendingGUI sendingGUI, SkullAPI skullAPI) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.config = config;
        this.miniMessage = miniMessage;
        this.userRepository = userRepository;
        this.sendingGUI = sendingGUI;
        this.skullAPI = skullAPI;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.config.guiSettings.parcelReceiverSelectionGuiTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.sendingGUI.show(player));
        GuiItem previousPageItem = this.config.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.config.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));

        gui.setItem(49, closeItem);

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.userRepository.findPage(page).thenAccept(result -> {
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

                this.scheduler.runTask(this.plugin, () -> gui.open(player));
            }).whenComplete(ExceptionHandler.handler());
        }).whenComplete(ExceptionHandler.handler());
    }

    private CompletableFuture<List<Supplier<GuiItem>>> loadSkulls(Player player, UserPageResult result, PaginatedGuiRefresher refresh) {
        return result.users().stream()
            //.filter(user -> !user.uuid().equals(player.getUniqueId()))
            .map(user -> this.skullAPI.getSkullData(user.uuid()).thenApply(skullData -> this.toItem(player, user, skullData, refresh)))
            .collect(CompletableFutures.joinList());
    }

    private Supplier<GuiItem> toItem(Player player, User user, SkullData skullData, PaginatedGuiRefresher refresher) {
        UUID uuid = user.uuid();

        return () -> ItemBuilder.skull()
            .texture(skullData.getValue())
            .name(this.miniMessage.deserialize(user.name()))
            .lore(uuid.equals(this.receiver) ? this.miniMessage.deserialize(this.config.guiSettings.parcelReceiverSetLine) : this.miniMessage.deserialize(this.config.guiSettings.parcelReceiverNotSetLine))
            .glow(uuid.equals(this.receiver))
            .asGuiItem(event -> {
                if (uuid.equals(this.receiver)) {
                    this.receiver = null;
                    refresher.refresh();
                    return;
                }

                this.receiver = uuid;
                refresher.refresh();
            });
    }

}
