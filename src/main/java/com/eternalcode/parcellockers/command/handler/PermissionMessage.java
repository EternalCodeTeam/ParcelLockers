package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.command.LiteInvocation;
import dev.rollczi.litecommands.command.permission.RequiredPermissions;
import dev.rollczi.litecommands.handle.PermissionHandler;
import org.bukkit.command.CommandSender;
import panda.utilities.text.Formatter;
import panda.utilities.text.Joiner;

public class PermissionMessage implements PermissionHandler<CommandSender> {

    private final NotificationAnnouncer announcer;
    private final PluginConfiguration config;

    public PermissionMessage(NotificationAnnouncer announcer, PluginConfiguration config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public void handle(CommandSender commandSender, LiteInvocation invocation, RequiredPermissions requiredPermissions) {
        String value = Joiner.on(", ")
            .join(requiredPermissions.getPermissions())
            .toString();

        Formatter formatter = new Formatter()
            .register("{PERMISSION}", value);

        this.announcer.sendMessage(commandSender, formatter.format(this.config.messages.noPermission));
    }
}
