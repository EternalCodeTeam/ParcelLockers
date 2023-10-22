package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.handle.InvalidUsageHandler;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.command.CommandSender;
import panda.utilities.text.Formatter;

import java.util.List;

public class InvalidUsage implements InvalidUsageHandler<CommandSender> {

    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;

    public InvalidUsage(NotificationAnnouncer announcer, PluginConfiguration config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public void handle(CommandSender sender, LiteInvocation invocation, Schematic schematic) {
        List<String> schematics = schematic.getSchematics();

        if (schematic.isOnlyFirst()) {
            this.announcer.sendMessage(sender, this.config.messages.parcelCommandUsage);
            return;
        }

        for (String scheme : schematics) {
            Formatter formatter = new Formatter()
                .register("{USAGE}", scheme);

            this.announcer.sendMessage(sender, formatter.format(this.config.messages.invalidUsage));
        }
    }
}
