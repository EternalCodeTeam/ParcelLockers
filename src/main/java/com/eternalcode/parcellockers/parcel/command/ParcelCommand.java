package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
import com.eternalcode.parcellockers.gui.implementation.remote.ParcelListGui;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.util.ParcelPlaceholderUtil;
import com.eternalcode.parcellockers.user.UserService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.List;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused", "ClassCanBeRecord"})
@Command(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final MainGui mainGUI;
    private final ParcelListGui parcelListGUI;
    private final ParcelService parcelService;
    private final UserService userService;

    public ParcelCommand(
            LockerRepository lockerRepository,
            NotificationAnnouncer announcer,
            PluginConfiguration config,
            MainGui mainGUI,
            ParcelListGui parcelListGUI,
            ParcelService parcelService,
            UserService userService
    ) {
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.config = config;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelService = parcelService;
        this.userService = userService;
    }

    @Execute(name = "list")
    void list(@Context Player player) {
        this.parcelListGUI.show(player);
    }

    @Async
    @Execute(name = "info")
    void info(@Context Player player, @Arg Parcel parcel) {
        List<String> messagesToSend = ParcelPlaceholderUtil.replaceParcelPlaceholders(parcel, this.config.messages.parcelInfoMessages, this.userService, this.lockerRepository);
        messagesToSend.forEach(message -> this.announcer.sendMessage(player, message));
    }

    @Execute(name = "delete")
    void delete(@Context Player player, @Arg Parcel parcel) {
        this.parcelService.remove(player, parcel);
    }

    @Execute(name = "gui")
    void gui(@Context Player player) {
        this.mainGUI.show(player);
    }
}
