package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.util.ParcelPlaceholderUtil;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import com.eternalcode.parcellockers.user.UserManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

import static com.eternalcode.commons.adventure.AdventureUtil.resetItalic;

public class ParcelListGui implements GuiView {

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);
    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final UserManager userManager;
    private final MainGui mainGUI;

    public ParcelListGui(
        Plugin plugin,
        Server server,
        MiniMessage miniMessage,
        PluginConfiguration config,
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        UserManager userManager,
        MainGui mainGUI
    ) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.userManager = userManager;
        this.mainGUI = mainGUI;
    }

    @Override
    public void show(Player player) {
        this.show(player, FIRST_PAGE);
    }

    private void show(Player player, Page page) {
        PaginatedGui gui = Gui.paginated()
            .title(resetItalic(this.miniMessage.deserialize(this.config.guiSettings.parcelListGuiTitle)))
            .disableAllInteractions()
            .rows(6)
            .create();

        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));
        GuiItem previousPageItem = this.config.guiSettings.previousPageItem.toGuiItem(event -> this.show(player, page.previous()));
        GuiItem nextPageItem = this.config.guiSettings.nextPageItem.toGuiItem(event -> this.show(player, page.next()));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.parcelRepository.findPage(page).thenAccept(result -> {
            if (result.parcels().isEmpty() && page.hasPrevious()) {
                this.show(player, page.previous());
                return;
            }

            ConfigItem item = this.config.guiSettings.parcelItem;

            for (Parcel parcel : result.parcels()) {
                ItemBuilder parcelItem = item.toBuilder();

                List<Component> newLore = ParcelPlaceholderUtil.replaceParcelPlaceholders(parcel, item.lore, this.userManager, this.lockerRepository).stream()
                    .map(line -> resetItalic(this.miniMessage.deserialize(line)))
                    .toList();
                parcelItem.lore(newLore);
                parcelItem.name(this.miniMessage.deserialize(item.name.replace("{NAME}", parcel.name())));

                gui.addItem(parcelItem.asGuiItem());
            }

            gui.setItem(49, closeItem);

            if (result.hasNextPage()) {
                gui.setItem(51, nextPageItem);
            }

            if (page.hasPrevious()) {
                gui.setItem(47, previousPageItem);
            }

            this.server.getScheduler().runTask(this.plugin, () -> gui.open(player));
        }).whenComplete(SentryExceptionHandler.handler());

    }
}
