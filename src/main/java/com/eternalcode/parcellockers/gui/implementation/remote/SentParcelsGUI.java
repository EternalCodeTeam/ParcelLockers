package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.parcellockers.configuration.implementation.ConfigItem;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.ExceptionHandler;
import com.eternalcode.parcellockers.user.UserManager;
import com.spotify.futures.CompletableFutures;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
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

public class SentParcelsGUI extends GuiView {

    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final MainGUI mainGUI;
    private final UserManager userManager;

    public SentParcelsGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepository parcelRepository, LockerRepository lockerRepository, MainGUI mainGUI, UserManager userManager) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.mainGUI = mainGUI;
        this.userManager = userManager;
    }

    @Override
    public void show(Player player) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.config.guiSettings.sentParcelsTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        ConfigItem parcelItem = this.config.guiSettings.parcelItem;
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);


        this.parcelRepository.findBySender(player.getUniqueId()).thenAccept(optionalParcels -> {
            List<Parcel> parcels = optionalParcels.orElse(Collections.emptyList());

            for (Parcel parcel : parcels) {
                ItemBuilder item = parcelItem.toBuilder();

                List<Component> newLore = this.replaceParcelPlaceholders(parcel, parcelItem.lore)
                    .stream()
                    .map(line -> this.miniMessage.deserialize(line))
                    .toList();
                item.lore(newLore);
                item.name(this.miniMessage.deserialize(parcelItem.name.replace("{NAME}", parcel.name())));

                gui.addItem(item.asGuiItem());
            }
            this.server.getScheduler().runTask(this.plugin, () -> gui.open(player));
        }).whenComplete(ExceptionHandler.handler());
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
        }
        else {
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
