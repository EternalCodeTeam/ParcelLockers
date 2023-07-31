package com.eternalcode.parcellockers.command;

import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.route.Route;
import org.bukkit.entity.Player;

@Route(name = "debug")
public class DebugCommand {

    private final ParcelLockerDatabaseService parcelLockerDatabaseService;

    public DebugCommand(ParcelLockerDatabaseService parcelLockerDatabaseService) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
    }

    @Execute(route = "positioncache")
    void execute(Player player) {
        player.sendMessage("Position cache: " + this.parcelLockerDatabaseService.positionCache().toString());
    }
}
