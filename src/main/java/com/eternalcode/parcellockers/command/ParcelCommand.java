package com.eternalcode.parcellockers.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.manager.ParcelManager;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.Arg;
import dev.rollczi.litecommands.argument.By;
import dev.rollczi.litecommands.command.async.Async;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.permission.Permission;
import dev.rollczi.litecommands.command.route.Route;
import org.bukkit.entity.Player;

@Route(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;
    private final ParcelManager parcelManager;

    public ParcelCommand(NotificationAnnouncer announcer, PluginConfiguration config, ParcelManager parcelManager) {
        this.announcer = announcer;
        this.config = config;
        this.parcelManager = parcelManager;
    }

    @Async
    @Execute(route = "list")
    void list(Player player) {
        // show player parcels
    }

    @Async
    @Execute(route = "info", min = 1)
    void info(Player player, @Arg @By("parcel") Parcel parcel) {
        // show target parcel info and delivery options
    }
}
