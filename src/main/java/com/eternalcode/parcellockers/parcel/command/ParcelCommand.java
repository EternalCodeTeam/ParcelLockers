package com.eternalcode.parcellockers.parcel.command;

import com.eternalcode.parcellockers.gui.implementation.remote.MainGui;
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

    public ParcelCommand(MainGui mainGUI) {
        this.mainGUI = mainGUI;
    }

    @Execute(name = "gui")
    void gui(@Sender Player player) {
        this.mainGUI.show(player);
    }
}
