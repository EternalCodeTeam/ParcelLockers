package com.eternalcode.parcellockers;

import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.notification.NoticeService;
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
    private final NoticeService noticeService;

    public ParcelLockersCommand(ConfigService configManager, PluginConfig config, NoticeService noticeService) {
        this.configManager = configManager;
        this.config = config;
        this.noticeService = noticeService;
    }

    @Execute(name = "reload")
    void reload(@Sender CommandSender sender) {
        this.configManager.reload();
        this.noticeService.create()
            .viewer(sender)
            .notice(messages -> messages.reload)
            .send();
    }

    @Execute(name = "give")
    void give(@Sender Player player) {
        ItemStack parcelItem = this.config.settings.parcelLockerItem.toGuiItem().getItemStack();
        player.getInventory().addItem(parcelItem);
    }
}
