package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.parcellockers.discord.DiscordLinkService;
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
public class DiscordUnlinkCommand {

    private final DiscordLinkService discordLinkService;
    private final NoticeService noticeService;

    public DiscordUnlinkCommand(
        DiscordLinkService discordLinkService,
        NoticeService noticeService
    ) {
        this.discordLinkService = discordLinkService;
        this.noticeService = noticeService;
    }

    @Execute
    void unlinkSelf(@Context Player player) {
        UUID playerUuid = player.getUniqueId();

        this.discordLinkService.findLinkByPlayer(playerUuid).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.notLinked);
                return;
            }

            this.discordLinkService.unlinkPlayer(playerUuid).thenAccept(success -> {
                if (success) {
                    this.noticeService.player(playerUuid, messages -> messages.discord.unlinkSuccess);
                } else {
                    this.noticeService.player(playerUuid, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }

    @Execute
    @Permission("parcellockers.admin")
    void unlinkPlayer(@Context CommandSender sender, @Arg Player targetPlayer) {
        UUID targetUuid = targetPlayer.getUniqueId();

        this.discordLinkService.findLinkByPlayer(targetUuid).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.playerNotLinked);
                return;
            }

            this.discordLinkService.unlinkPlayer(targetUuid).thenAccept(success -> {
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
    void unlinkByDiscordId(@Context CommandSender sender, @Arg Snowflake discordId) {
        String discordIdString = discordId.asString();

        this.discordLinkService.findLinkByDiscordId(discordIdString).thenAccept(optionalLink -> {
            if (optionalLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.discordNotLinked);
                return;
            }

            this.discordLinkService.unlinkDiscordId(discordIdString).thenAccept(success -> {
                if (success) {
                    this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkByDiscordSuccess);
                } else {
                    this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }
}
