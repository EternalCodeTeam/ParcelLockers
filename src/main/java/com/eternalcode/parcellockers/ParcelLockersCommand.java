package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Command(name = "parcellockers", aliases = {"parcellocker"})
@Permission("parcellockers.admin")
public class ParcelLockersCommand {

    private final ConfigurationManager configManager;
    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final MiniMessage miniMessage;

    public ParcelLockersCommand(ConfigurationManager configManager, PluginConfiguration config, NotificationAnnouncer announcer, MiniMessage miniMessage) {
        this.configManager = configManager;
        this.config = config;
        this.announcer = announcer;
        this.miniMessage = miniMessage;
    }
    
    @Async
    @Execute(name = "reload", aliases = {"rl"})
    void reload(@Context CommandSender sender) {
        this.configManager.reload();
        this.announcer.sendMessage(sender, this.config.messages.reload);
    }

    @Execute(name = "give")
    void give(@Context Player player) {
        ItemStack parcelItem = this.config.settings.parcelLockerItem.toGuiItem(this.miniMessage).getItemStack();
        player.getInventory().addItem(parcelItem);
    }

}
