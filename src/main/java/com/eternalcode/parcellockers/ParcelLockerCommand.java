package com.eternalcode.parcellockers;

import dev.rollczi.litecommands.command.async.Async;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.permission.Permission;
import dev.rollczi.litecommands.command.route.Route;

@Route(name = "parcellockers", aliases = {"parcellocker"})
@Permission("parcellockers.admin")
public class ParcelLockerCommand {

    @Async
    @Execute
    void reload() {
        // TODO reload logic
    }

}
