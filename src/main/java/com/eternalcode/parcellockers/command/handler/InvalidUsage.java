package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.handle.InvalidUsageHandler;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.command.CommandSender;

import java.util.List;

public class InvalidUsage implements InvalidUsageHandler<CommandSender> {

    private final NotificationAnnouncer announcer;

    public InvalidUsage(NotificationAnnouncer announcer) {
        this.announcer = announcer;
    }

    @Override
    public void handle(CommandSender sender, LiteInvocation invocation, Schematic schematic) {
        List<String> schematics = schematic.getSchematics();

        if (schematic.isOnlyFirst()) {
            this.announcer.sendMessage(sender, "&cInvalid usage! Correct usage: " + schematics.get(0));
            return;
        }

        this.announcer.sendMessage(sender, "Invalid usage! Correct usages: ");
        for (String sch : schematics) {
            this.announcer.sendMessage(sender, " - " + sch);
        }
    }
}
