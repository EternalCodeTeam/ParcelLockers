package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.command.CommandSender;
import panda.utilities.text.Formatter;

import java.util.List;

public class InvalidUsageImpl implements InvalidUsageHandler<CommandSender> {

    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;

    public InvalidUsageImpl(NotificationAnnouncer announcer, PluginConfiguration config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public void handle(Invocation<CommandSender> invocation, InvalidUsage<CommandSender> result, ResultHandlerChain<CommandSender> chain) {
        Schematic schematic = result.getSchematic();
        List<String> schematics = schematic.all();

        if (schematic.isOnlyFirst()) {
            this.announcer.sendMessage(invocation.sender(), this.config.messages.parcelCommandUsage);
            return;
        }

        for (String scheme : schematics) {
            Formatter formatter = new Formatter()
                    .register("{USAGE}", scheme);

            this.announcer.sendMessage(invocation.sender(), formatter.format(this.config.messages.invalidUsage));
        }
    }

}
