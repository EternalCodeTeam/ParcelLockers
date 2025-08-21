package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Sender;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Command(name = "parcellockers")
@Permission("parcellockers.admin")
public class ParcelLockersCommand {

    private final ConfigService configManager;
    private final PluginConfig config;
    private final NotificationAnnouncer announcer;

    public ParcelLockersCommand(ConfigService configManager, PluginConfig config, NotificationAnnouncer announcer) {
        this.configManager = configManager;
        this.config = config;
        this.announcer = announcer;
    }

    @Execute(name = "reload")
    void reload(@Sender CommandSender sender) {
        this.configManager.reload();
        this.announcer.sendMessage(sender, this.config.messages.reload);
    }

    @Execute(name = "give")
    void give(@Sender Player player) {
        ItemStack parcelItem = this.config.settings.parcelLockerItem.toGuiItem().getItemStack();
        player.getInventory().addItem(parcelItem);
    }
}
