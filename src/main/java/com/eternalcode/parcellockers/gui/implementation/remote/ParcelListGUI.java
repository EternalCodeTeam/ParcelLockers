package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.UserManager;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.sentry.Sentry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Blocking;
import panda.utilities.text.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelListGUI extends GuiView {

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
    private final MainGUI mainGUI;

    public ParcelListGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepository parcelRepository, LockerRepository lockerRepository, UserManager userManager, MainGUI mainGUI) {
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
            .title(this.miniMessage.deserialize(this.config.guiSettings.parcelListGuiTitle))
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

        this.parcelRepository.findPage(page).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Sentry.captureException(throwable);
                throwable.printStackTrace();
                return;
            }

            if (result.parcels().isEmpty() && page.hasPrevious()) {
                this.show(player, page.previous());
                return;
            }

            ConfigItem item = this.config.guiSettings.parcelItem;

            for (Parcel parcel : result.parcels()) {
                ItemBuilder parcelItem = item.toBuilder();

                List<Component> newLore = this.replaceParcelPlaceholders(parcel, item.lore).stream()
                    .map(line -> this.miniMessage.deserialize(line))
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
        }).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                Sentry.captureException(throwable);
                throwable.printStackTrace();
            }
        });

    }

    @Blocking
    private List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        String senderName = this.getName(parcel.sender()).join();
        String receiver = this.getName(parcel.receiver()).join();

        List<String> recipients = parcel.recipients().stream()
            .map(uuid -> this.getName(uuid))
            .collect(CompletableFutures.joinList())
            .join();

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", senderName)
            .register("{RECEIVER}", receiver)
            .register("{SIZE}", parcel.size().toString())
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description())
            .register("{RECIPIENTS}", recipients.toString());

        Optional<Locker> lockerOptional = this.lockerRepository.findByUUID(parcel.destinationLocker()).join();

        if (lockerOptional.isPresent()) {
            Locker locker = lockerOptional.get();
            formatter.register("{POSITION_X}", locker.position().x())
                .register("{POSITION_Y}", locker.position().y())
                .register("{POSITION_Z}", locker.position().z());
        } else {
            formatter.register("{POSITION_X}", "-")
                .register("{POSITION_Y}", "-")
                .register("{POSITION_Z}", "-");
        }

        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            newLore.add(formatter.format(line));
        }

        return newLore;
    }

    private CompletableFuture<String> getName(UUID userUuid) {
        return this.userManager.getUser(userUuid).thenApply(userOptional -> userOptional
            .map(user -> user.name())
            .orElse("Unknown")
        );
    }

}
