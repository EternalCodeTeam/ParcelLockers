package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
import com.eternalcode.parcellockers.gui.implementation.remote.ParcelListGui;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Sender;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused", "ClassCanBeRecord"})
@Command(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    private final MainGui mainGUI;
    private final ParcelListGui parcelListGUI;
    private final ParcelService parcelService;

    public ParcelCommand(
            MainGui mainGUI,
            ParcelListGui parcelListGUI,
            ParcelService parcelService
    ) {
        this.mainGUI = mainGUI;
        this.parcelListGUI = parcelListGUI;
        this.parcelService = parcelService;
    }

    @Execute(name = "list")
    void list(@Sender Player player) {
        this.parcelListGUI.show(player);
    }

    @Execute(name = "delete")
    void delete(@Sender Player player, @Arg Parcel parcel) {
        this.parcelService.remove(player, parcel);
    }

    @Execute(name = "gui")
    void gui(@Sender Player player) {
        this.mainGUI.show(player);
    }
}
