package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.locker.gui.MainGUI;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.gui.ParcelListGUI;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import panda.utilities.text.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Command(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final Server server;
    private final LockerRepositoryImpl lockerRepository;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final MainGUI mainGUI;
    private final ParcelListGUI parcelListGUI;
    private final ParcelManager parcelManager;

    public ParcelCommand(Server server, LockerRepositoryImpl lockerRepository, NotificationAnnouncer announcer, PluginConfiguration config, MainGUI mainGUI, ParcelListGUI parcelListGUI, ParcelManager parcelManager) {
        this.server = server;
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.config = config;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelManager = parcelManager;
    }

    @Execute(name = "list")
    void list(@Context Player player) {
        this.parcelListGUI.show(player);
    }

    @Execute(name = "info")
    void info(@Context Player player, @Arg Parcel parcel) {
        List<String> messagesToSend = this.replaceParcelPlaceholders(parcel, this.config.messages.parcelInfoMessages);
        messagesToSend.forEach(message -> this.announcer.sendMessage(player, message));
    }

    @Execute(name = "send", aliases = "create") // similar create, add
    void create(@Context Player player, @Arg String name, @Arg boolean priority, @Arg ParcelSize size, @Arg UUID entryLocker, @Arg UUID destinationLocker) {
        Parcel parcel = Parcel.builder()
            .uuid(UUID.randomUUID())
            .name(name)
            .sender(player.getUniqueId())
            .receiver(player.getUniqueId())
            .priority(priority)
            .size(size)
            .entryLocker(entryLocker)
            .destinationLocker(destinationLocker)
            .build();

        this.parcelManager.createParcel(player, parcel);
    }

    @Execute(name = "cancel") // similar remove, delete
    void delete(@Context Player player, @Arg Parcel parcel) {
        this.parcelManager.deleteParcel(player, parcel);
    }

    @Execute(name = "gui")
    void gui(@Context Player player) {
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
