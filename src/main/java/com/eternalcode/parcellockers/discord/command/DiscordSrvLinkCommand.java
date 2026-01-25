package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.parcellockers.discord.DiscordSrvLinkService;
import com.eternalcode.parcellockers.notification.NoticeService;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;


@Command(name = "parcel linkdiscord")
public class DiscordSrvLinkCommand {

    private final DiscordSrvLinkService discordSrvLinkService;
    private final NoticeService noticeService;

    public DiscordSrvLinkCommand(
        DiscordSrvLinkService discordSrvLinkService,
        NoticeService noticeService
    ) {
        this.discordSrvLinkService = discordSrvLinkService;
        this.noticeService = noticeService;
    }

    @Execute
    void linkSelf(@Context Player player) {
        UUID playerUuid = player.getUniqueId();

        this.discordSrvLinkService.findLinkByPlayer(playerUuid).thenAccept(optionalLink -> {
            if (optionalLink.isPresent()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.discordSrvAlreadyLinked);
                return;
            }

            Optional<String> linkingCode = this.discordSrvLinkService.getLinkingCode(playerUuid);
            if (linkingCode.isEmpty()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.discordSrvAlreadyLinked);
                return;
            }

            this.noticeService.create()
                .notice(messages -> messages.discord.discordSrvLinkRedirect)
                .placeholder("{CODE}", linkingCode.get())
                .player(playerUuid)
                .send();
        });
    }
}
