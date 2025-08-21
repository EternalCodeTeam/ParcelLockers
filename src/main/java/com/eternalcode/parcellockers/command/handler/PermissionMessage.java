package com.eternalcode.parcellockers.command.handler;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.permission.MissingPermissions;
import dev.rollczi.litecommands.permission.MissingPermissionsHandler;
import org.bukkit.command.CommandSender;
import panda.utilities.text.Formatter;

public class PermissionMessage implements MissingPermissionsHandler<CommandSender> {

    private final NotificationAnnouncer announcer;
    private final PluginConfig config;

    public PermissionMessage(NotificationAnnouncer announcer, PluginConfig config) {
        this.announcer = announcer;
        this.config = config;
    }

    @Override
    public void handle(Invocation<CommandSender> invocation, MissingPermissions missingPermissions, ResultHandlerChain<CommandSender> chain) {
        String permissions = missingPermissions.asJoinedText();

        Formatter formatter = new Formatter()
            .register("{PERMISSION}", permissions);

        this.announcer.sendMessage(invocation.sender(), formatter.format(this.config.messages.noPermission));
    }
}
