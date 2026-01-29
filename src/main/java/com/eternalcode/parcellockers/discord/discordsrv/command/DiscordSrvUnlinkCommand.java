package com.eternalcode.parcellockers.discord.discordsrv.command;

import com.eternalcode.parcellockers.discord.discordsrv.DiscordSrvLinkService;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import discord4j.common.util.Snowflake;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        this.discordSrvLinkService.findLinkByPlayer(playerUuid).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
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

        this.discordSrvLinkService.findLinkByPlayer(targetUuid).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.playerNotLinked);
                return;
            }

            this.discordSrvLinkService.unlinkPlayer(targetUuid).thenAccept(result -> {
                switch (result) {
                    case SUCCESS -> {
                        this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkSuccess);
                        this.noticeService.player(targetUuid, messages -> messages.discord.unlinkSuccess);
                    }
                    case NOT_LINKED -> this.noticeService.viewer(sender, messages -> messages.discord.playerNotLinked);
                    case GENERIC_FAILURE -> this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }

    @Execute
    @Permission("parcellockers.admin")
    void unlinkByDiscordId(@Context CommandSender sender, @Arg Snowflake discordId) {
        long discordIdLong = discordId.asLong();

        this.discordSrvLinkService.findLinkByDiscordId(discordIdLong).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.discordNotLinked);
                return;
            }

            this.discordSrvLinkService.unlinkDiscordId(discordIdLong).thenAccept(result -> {
                switch (result) {
                    case SUCCESS -> this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkByDiscordSuccess);
                    case NOT_LINKED -> this.noticeService.viewer(sender, messages -> messages.discord.discordNotLinked);
                    case GENERIC_FAILURE -> this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }
}
