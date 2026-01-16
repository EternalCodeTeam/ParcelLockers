package com.eternalcode.parcellockers.discord.command;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "parcel discordlink")
public class DiscordLinkCommand {

    @Execute
    void execute(@Context Player player, @Arg long discordId) {
        // Implementation for linking Discord ID to Minecraft player goes here
    }

    @Execute
    @Permission("parcellockers.admin")
    void admin(@Context CommandSender sender, @Arg Player player, @Arg long discordId) {

    }

}
