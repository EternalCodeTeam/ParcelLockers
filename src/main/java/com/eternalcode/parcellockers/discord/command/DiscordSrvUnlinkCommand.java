package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.parcellockers.discord.DiscordSrvLinkService;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for unlinking Discord accounts when DiscordSRV is installed.
 * Regular players are redirected to use DiscordSRV's unlinking system,
 * while admins can still forcefully unlink accounts.
 */
@Command(name = "parcel unlinkdiscord")
public class DiscordSrvUnlinkCommand {

    private final DiscordSrvLinkService discordSrvLinkService;
    private final NoticeService noticeService;

    public DiscordSrvUnlinkCommand(
        DiscordSrvLinkService discordSrvLinkService,
        NoticeService noticeService
    ) {
        this.discordSrvLinkService = discordSrvLinkService;
        this.noticeService = noticeService;
    }

    @Execute
    void unlinkSelf(@Context Player player) {
        UUID playerUuid = player.getUniqueId();

        this.discordSrvLinkService.findLinkByPlayer(playerUuid).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.notLinked);
                return;
            }

            // Redirect to DiscordSRV's unlinking system
            this.noticeService.player(playerUuid, messages -> messages.discord.discordSrvUnlinkRedirect);
        });
    }

    @Execute
    @Permission("parcellockers.admin")
    void unlinkPlayer(@Context CommandSender sender, @Arg Player targetPlayer) {
        UUID targetUuid = targetPlayer.getUniqueId();

        this.discordSrvLinkService.findLinkByPlayer(targetUuid).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.playerNotLinked);
                return;
            }

            this.discordSrvLinkService.unlinkPlayer(targetUuid).thenAccept(success -> {
                if (success) {
                    this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkSuccess);
                    this.noticeService.player(targetUuid, messages -> messages.discord.unlinkSuccess);
                } else {
                    this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }

    @Execute
    @Permission("parcellockers.admin")
    void unlinkByDiscordId(@Context CommandSender sender, @Arg long discordId) {
        String discordIdString = String.valueOf(discordId);

        this.discordSrvLinkService.findLinkByDiscordId(discordIdString).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.discordNotLinked);
                return;
            }

            this.discordSrvLinkService.unlinkDiscordId(discordIdString).thenAccept(success -> {
                if (success) {
                    this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkByDiscordSuccess);
                } else {
                    this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }
}
