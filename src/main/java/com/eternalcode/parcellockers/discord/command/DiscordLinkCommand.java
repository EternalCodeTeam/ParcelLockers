package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.commons.concurrent.FutureHandler;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.discord.verification.DiscordLinkValidationService;
import com.eternalcode.parcellockers.discord.verification.DiscordVerificationService;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import discord4j.common.util.Snowflake;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "parcel linkdiscord")
public class DiscordLinkCommand {

    private final DiscordLinkService discordLinkService;
    private final DiscordLinkValidationService validationService;
    private final DiscordVerificationService verificationService;
    private final NoticeService noticeService;

    public DiscordLinkCommand(
        DiscordLinkService discordLinkService,
        DiscordLinkValidationService validationService,
        DiscordVerificationService verificationService,
        NoticeService noticeService
    ) {
        this.discordLinkService = discordLinkService;
        this.validationService = validationService;
        this.verificationService = verificationService;
        this.noticeService = noticeService;
    }

    @Execute
    void linkSelf(@Context Player player, @Arg Snowflake discordId) {
        UUID playerUuid = player.getUniqueId();
        long discordIdLong = discordId.asLong();

        if (this.verificationService.hasPendingVerification(playerUuid)) {
            this.noticeService.player(playerUuid, messages -> messages.discord.verificationAlreadyPending);
            return;
        }

        this.validationService.validate(playerUuid, discordIdLong)
            .thenCompose(validationResult -> {
                if (!validationResult.isValid()) {
                    this.sendValidationError(playerUuid, validationResult);
                    return CompletableFuture.completedFuture(null);
                }

                return this.validationService.getDiscordUser(discordIdLong)
                    .thenCompose(discordUser ->
                        this.verificationService.startVerification(player, discordIdLong, discordUser).toFuture()
                    );
            })
            .exceptionally(error -> {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
                return null;
            });
    }

    @Execute
    @Permission("parcellockers.admin")
    void linkOther(@Context CommandSender sender, @Arg OfflinePlayer player, @Arg Snowflake discordId) {
        long discordIdLong = discordId.asLong();
        UUID playerUuid = player.getUniqueId();

        this.validationService.validate(playerUuid, discordIdLong)
            .thenCompose(validationResult -> {
                if (!validationResult.isValid()) {
                    this.sendValidationErrorToViewer(sender, validationResult);
                    return CompletableFuture.completedFuture(null);
                }

                return this.discordLinkService.createLink(playerUuid, discordIdLong)
                    .thenAccept(success -> {
                        if (success) {
                            this.noticeService.viewer(sender, messages -> messages.discord.adminLinkSuccess);
                            this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
                            return;
                        }
                        this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
                    });
            })
            .exceptionally(error -> {
                this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
                return FutureHandler.handleException(error);
            });
    }

    private void sendValidationError(UUID playerUuid, ValidationResult result) {
        switch (result.errorMessage()) {
            case "alreadyLinked" -> this.noticeService.player(playerUuid, messages -> messages.discord.alreadyLinked);
            case "discordAlreadyLinked" -> this.noticeService.player(playerUuid, messages -> messages.discord.discordAlreadyLinked);
            case "userNotFound" -> this.noticeService.player(playerUuid, messages -> messages.discord.userNotFound);
            default -> this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
        }
    }

    private void sendValidationErrorToViewer(CommandSender sender, ValidationResult result) {
        switch (result.errorMessage()) {
            case "alreadyLinked" -> this.noticeService.viewer(sender, messages -> messages.discord.playerAlreadyLinked);
            case "discordAlreadyLinked" -> this.noticeService.viewer(sender, messages -> messages.discord.discordAlreadyLinked);
            case "userNotFound" -> this.noticeService.viewer(sender, messages -> messages.discord.userNotFound);
            default -> this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
        }
    }
}

