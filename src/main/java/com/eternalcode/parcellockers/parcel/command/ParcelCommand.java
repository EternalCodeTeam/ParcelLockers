package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
import com.eternalcode.parcellockers.gui.implementation.remote.ParcelListGui;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelManager;
import com.eternalcode.parcellockers.parcel.util.ParcelPlaceholderUtil;
import com.eternalcode.parcellockers.user.UserManager;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;

import java.util.List;

@Command(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final LockerRepository lockerRepository;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final MainGui mainGUI;
    private final ParcelListGui parcelListGUI;
    private final ParcelManager parcelManager;
    private final UserManager userManager;

    public ParcelCommand(LockerRepository lockerRepository, NotificationAnnouncer announcer, PluginConfiguration config, MainGui mainGUI, ParcelListGui parcelListGUI, ParcelManager parcelManager, UserManager userManager) {
        this.lockerRepository = lockerRepository;
        this.announcer = announcer;
        this.config = config;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelManager = parcelManager;
        this.userManager = userManager;
    }

    @Execute(name = "list")
    void list(@Context Player player) {
        this.parcelListGUI.show(player);
    }

    @Async
    @Execute(name = "info")
    void info(@Context Player player, @Arg Parcel parcel) {
        List<String> messagesToSend = ParcelPlaceholderUtil.replaceParcelPlaceholders(parcel, this.config.messages.parcelInfoMessages, this.userManager, this.lockerRepository);
        messagesToSend.forEach(message -> this.announcer.sendMessage(player, message));
    }

    @Execute(name = "delete")
    void delete(@Context Player player, @Arg Parcel parcel) {
        this.parcelManager.deleteParcel(player, parcel);
    }

    @Execute(name = "gui")
    void gui(@Context Player player) {
        this.mainGUI.show(player);
    }
}
