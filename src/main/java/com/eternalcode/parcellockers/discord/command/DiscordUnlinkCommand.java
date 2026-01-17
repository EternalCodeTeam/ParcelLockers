package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "parcel unlinkdiscord")
public class DiscordUnlinkCommand {

    private final DiscordLinkRepository discordLinkRepository;
    private final NoticeService noticeService;

    public DiscordUnlinkCommand(
        DiscordLinkRepository discordLinkRepository,
        NoticeService noticeService
    ) {
        this.discordLinkRepository = discordLinkRepository;
        this.noticeService = noticeService;
    }

    @Execute
    void unlinkSelf(@Context Player player) {
        UUID playerUuid = player.getUniqueId();

        this.discordLinkRepository.findByPlayerUuid(playerUuid).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.notLinked);
                return;
            }

            this.discordLinkRepository.deleteByPlayerUuid(playerUuid).thenAccept(success -> {
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

        this.discordLinkRepository.findByPlayerUuid(targetUuid).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.playerNotLinked);
                return;
            }

            this.discordLinkRepository.deleteByPlayerUuid(targetUuid).thenAccept(success -> {
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

        this.discordLinkRepository.findByDiscordId(discordIdString).thenAccept(existingLink -> {
            if (existingLink.isEmpty()) {
                this.noticeService.viewer(sender, messages -> messages.discord.discordNotLinked);
                return;
            }

            this.discordLinkRepository.deleteByDiscordId(discordIdString).thenAccept(success -> {
                if (success) {
                    this.noticeService.viewer(sender, messages -> messages.discord.adminUnlinkByDiscordSuccess);
                } else {
                    this.noticeService.viewer(sender, messages -> messages.discord.unlinkFailed);
                }
            });
        });
    }
}
