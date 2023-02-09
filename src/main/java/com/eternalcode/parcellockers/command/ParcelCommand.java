package com.eternalcode.parcellockers.command;

import dev.rollczi.litecommands.command.async.Async;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.permission.Permission;
import dev.rollczi.litecommands.command.route.Route;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Route(name = "parcel")
@Permission("parcellockers.command.parcel")
public class ParcelCommand {

    @Async
    @Execute
    void execute(Player player) {
        player.sendMessage(Component.text(ChatColor.AQUA + "Usage: /parcel <create|remove|info>"));
    }
}
