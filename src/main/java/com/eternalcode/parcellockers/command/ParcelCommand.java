package com.eternalcode.parcellockers.command;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.gui.MainGUI;
import com.eternalcode.parcellockers.gui.ParcelListGUI;
import com.eternalcode.parcellockers.manager.ParcelManager;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.Parcel;
import dev.rollczi.litecommands.argument.Arg;
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
    private final MainGUI mainGUI;
    private final ParcelListGUI parcelListGUI;

    public ParcelCommand(NotificationAnnouncer announcer, PluginConfiguration config, ParcelManager parcelManager, MainGUI mainGUI, ParcelListGUI parcelListGUI) {
        this.announcer = announcer;
        this.config = config;
        this.parcelManager = parcelManager;
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
    }

    @Execute(route = "list")
    void list(Player player) {
        this.parcelListGUI.showParcelListGUI(player);
    }

    @Execute(route = "info")
    void info(Player player, @Arg Parcel parcel) {
        // show target parcel info and delivery options
    }

    @Execute(route = "gui")
    void gui(Player player) {
        this.mainGUI.showMainGUI(player);
    }

    @Execute(route = "create-test")
    void createTestParcel(Player player) {
        this.parcelManager.saveTestParcel();
        this.announcer.sendMessage(player, "&aDone.");
    }

    @Execute(route = "list-all")
    void listAll(Player player) {
        this.parcelManager.listAll(player);
    }
}
