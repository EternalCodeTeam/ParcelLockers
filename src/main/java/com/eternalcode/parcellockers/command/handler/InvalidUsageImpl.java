package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import java.util.List;
import org.bukkit.command.CommandSender;
import panda.utilities.text.Formatter;

public class InvalidUsageImpl implements InvalidUsageHandler<CommandSender> {

    private final NotificationAnnouncer announcer;
    private final PluginConfig config;

    public InvalidUsageImpl(NotificationAnnouncer announcer, PluginConfig config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public void handle(Invocation<CommandSender> invocation, InvalidUsage<CommandSender> result, ResultHandlerChain<CommandSender> chain) {
        Schematic schematic = result.getSchematic();

        if (schematic.isOnlyFirst()) {
            this.announcer.sendMessage(invocation.sender(), this.config.messages.parcelCommandUsage);
            return;
        }

        List<String> schematics = schematic.all();

        for (String scheme : schematics) {
            Formatter formatter = new Formatter()
                .register("{USAGE}", scheme);

            this.announcer.sendMessage(invocation.sender(), formatter.format(this.config.messages.invalidUsage));
        }
    }

}
