package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.command.async.Async;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.permission.Permission;
import dev.rollczi.litecommands.command.route.Route;
import org.bukkit.command.CommandSender;

@Route(name = "parcellockers", aliases = {"parcellocker"})
@Permission("parcellockers.admin")
public class ParcelLockerCommand {

    private final ConfigurationManager configManager;
    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;

    public ParcelLockerCommand(ConfigurationManager configManager, PluginConfiguration config, NotificationAnnouncer announcer) {
        this.configManager = configManager;
        this.config = config;
        this.announcer = announcer;
    }
    
    @Async
    @Execute(route = "reload", aliases = {"rl"})
    void reload(CommandSender sender) {
        this.configManager.reload();
        this.announcer.sendMessage(sender, this.config.messages.reload);
    }

}
