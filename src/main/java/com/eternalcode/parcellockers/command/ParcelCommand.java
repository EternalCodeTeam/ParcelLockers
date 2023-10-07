package com.eternalcode.parcellockers.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.locker.gui.MainGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.gui.ParcelListGUI;
import dev.rollczi.litecommands.argument.Arg;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.permission.Permission;
import dev.rollczi.litecommands.command.route.Route;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import panda.utilities.text.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Route(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final Server server;
    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final MainGUI mainGUI;
    private final ParcelListGUI parcelListGUI;
    private final ParcelManager parcelManager;

    public ParcelCommand(Server server, LockerRepository lockerRepository, NotificationAnnouncer announcer, PluginConfiguration config, MainGUI mainGUI, ParcelListGUI parcelListGUI, ParcelManager parcelManager) {
        this.server = server;
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.config = config;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelManager = parcelManager;
    }

    @Execute(route = "list")
    void list(Player player) {
        this.parcelListGUI.show(player);
    }

    @Execute(route = "info")
    void info(Player player, @Arg Parcel parcel) {
        List<String> messagesToSend = this.replaceParcelPlaceholders(parcel, this.config.messages.parcelInfoMessages);
        messagesToSend.forEach(message -> this.announcer.sendMessage(player, message));
    }

    @Execute(route = "create", aliases = { "add", "send" })
    void create(Player player, @Arg String name, @Arg boolean priority, @Arg String size, @Arg String entryLocker, @Arg String destinationLocker) {
        Parcel parcel = Parcel.builder()
            .uuid(UUID.randomUUID())
            .name(name)
            .sender(player.getUniqueId())
            .receiver(player.getUniqueId())
            .priority(priority)
            .size(ParcelSize.valueOf(size.toUpperCase()))
            .entryLocker(UUID.fromString(entryLocker))
            .destinationLocker(UUID.fromString(destinationLocker))
            .build();

        this.parcelManager.createParcel(player, parcel);
    }

    @Execute(route = "delete", aliases = { "remove", "cancel" })
    void delete(Player player, @Arg Parcel parcel) {
        this.parcelManager.deleteParcel(player, parcel);
    }

    @Execute(route = "gui")
    void gui(Player player) {
        this.mainGUI.show(player);
    }

    public List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        Formatter formatter = new Formatter()
            .register("{UUID}", parcel.uuid().toString())
            .register("{NAME}", parcel.name())
            .register("{SENDER}", this.server.getPlayer(parcel.sender()).getName())
            .register("{RECEIVER}", this.server.getPlayer(parcel.receiver()).getName())
            .register("{SIZE}", parcel.size().toString())
            .register("{PRIORITY}", parcel.priority() ? "&aYes" : "&cNo")
            .register("{DESCRIPTION}", parcel.description())
            .register("{RECIPIENTS}", parcel.recipients().stream()
                .map(this.server::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getName)
                .toList()
                .toString()
            );

        this.lockerRepository.findByUUID(parcel.destinationLocker()).join().ifPresent(locker -> formatter
            .register("{POSITION_X}", locker.position().x())
            .register("{POSITION_Y}", locker.position().y())
            .register("{POSITION_Z}", locker.position().z())
        );

        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            formatter.format(line);
            newLore.add(line);
        }

        return newLore;
    }
}
