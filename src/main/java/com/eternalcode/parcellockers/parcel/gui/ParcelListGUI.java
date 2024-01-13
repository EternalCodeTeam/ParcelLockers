package com.eternalcode.parcellockers.parcel.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.gui.MainGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import com.eternalcode.parcellockers.shared.Page;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.sentry.Sentry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import panda.utilities.text.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ParcelListGUI extends GuiView {

    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepositoryImpl parcelRepository;
    private final LockerRepositoryImpl lockerRepository;
    private final MainGUI mainGUI;

    private static final int WIDTH = 7;
    private static final int HEIGHT = 4;
    private static final Page FIRST_PAGE = new Page(0, WIDTH * HEIGHT);

    public ParcelListGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepositoryImpl parcelRepository, LockerRepositoryImpl lockerRepository, MainGUI mainGUI) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
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
            
        GuiItem parcelItem = this.config.guiSettings.parcelItem.toGuiItem();
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

            for (Parcel parcel : result.parcels()) {
                /*if (!parcel.recipients().contains(player.getUniqueId())) {
                    continue;
                }*/

                ItemMeta parcelItemMeta = parcelItem.getItemStack().getItemMeta();

                if (parcelItemMeta != null) {
                    List<String> newLore = this.replaceParcelPlaceholders(parcel, parcelItemMeta.getLore());
                    parcelItemMeta.setLore(newLore);
                }
                gui.addItem(parcelItem);
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

    private List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", this.server.getPlayer(parcel.sender()).getName())
            .register("{RECEIVER}", this.server.getPlayer(parcel.receiver()).getName())
            .register("{SIZE}", parcel.size().toString())
            .register("{PRIORITY}", parcel.priority() ? this.miniMessage.deserialize("&aYes") : this.miniMessage.deserialize("&cNo"))
            //.register("{DESCRIPTION}", parcel.description())
            .register("{RECIPIENTS}", parcel.recipients().stream()
                .map(this.server::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getName)
                .toList()
                .toString()
            );

        UUID destinationLocker = parcel.destinationLocker();

        this.lockerRepository.findLocker(destinationLocker).ifPresent(locker -> formatter
            .register("{POSITION_X}", locker.position().x())
            .register("{POSITION_Y}", locker.position().y())
            .register("{POSITION_Z}", locker.position().z())
        );

        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            newLore.add(formatter.format(line));
        }

        return newLore;
    }

}
