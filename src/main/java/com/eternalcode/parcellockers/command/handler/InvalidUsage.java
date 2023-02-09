package com.eternalcode.parcellockers.command.handler;

import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.handle.InvalidUsageHandler;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class InvalidUsage implements InvalidUsageHandler<CommandSender> {

    @Override
    public void handle(CommandSender sender, LiteInvocation invocation, Schematic schematic) {
        List<String> schematics = schematic.getSchematics();

        if (schematics.size() == 0) {
            sender.sendMessage(ChatColor.RED + "Correct usage: /parcel <create|remove|info>");
            return;
        }

        sender.sendMessage(ChatColor.RED + "Invalid usage! Correct usages: ");
        for (String sch : schematics) {
            sender.sendMessage(" - " + sch);
        }
    }
}
