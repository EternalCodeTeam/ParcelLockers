package com.eternalcode.parcellockers.discord.verification;

import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.notification.NoticeService;
import discord4j.core.object.entity.User;
import io.papermc.paper.dialog.Dialog;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import reactor.core.publisher.Mono;

public class DiscordVerificationService {

    private final VerificationCache verificationCache;
    private final VerificationCodeGenerator codeGenerator;
    private final DiscordVerificationDialogFactory dialogFactory;
    private final DiscordLinkService discordLinkService;
    private final NoticeService noticeService;
    private final MessageConfig messageConfig;

    private DiscordVerificationService(
        VerificationCache verificationCache,
        VerificationCodeGenerator codeGenerator,
        DiscordVerificationDialogFactory dialogFactory,
        DiscordLinkService discordLinkService,
        NoticeService noticeService,
        MessageConfig messageConfig
    ) {
        this.verificationCache = verificationCache;
        this.codeGenerator = codeGenerator;
        this.dialogFactory = dialogFactory;
        this.discordLinkService = discordLinkService;
        this.noticeService = noticeService;
        this.messageConfig = messageConfig;
    }

    public static DiscordVerificationService create(
        DiscordLinkService discordLinkService,
        NoticeService noticeService,
        MessageConfig messageConfig,
        MiniMessage miniMessage
    ) {
        VerificationCache verificationCache = new VerificationCache();
        VerificationCodeGenerator codeGenerator = new VerificationCodeGenerator();
        DiscordVerificationDialogFactory dialogFactory =
            new DiscordVerificationDialogFactory(miniMessage, messageConfig);

        return new DiscordVerificationService(
            verificationCache,
            codeGenerator,
            dialogFactory,
            discordLinkService,
            noticeService,
            messageConfig
        );
    }

    public boolean hasPendingVerification(UUID playerUuid) {
        return this.verificationCache.hasPendingVerification(playerUuid);
    }

    public Mono<Void> startVerification(Player player, long discordId, User discordUser) {
        UUID playerUuid = player.getUniqueId();
        String code = this.codeGenerator.generate();

        VerificationData data = new VerificationData(discordId, code);
        if (!this.verificationCache.putIfAbsent(playerUuid, data)) {
            this.noticeService.player(playerUuid, messages -> messages.discord.verificationAlreadyPending);
            return Mono.empty();
        }

        return discordUser.getPrivateChannel()
            .flatMap(channel -> channel.createMessage(
                this.messageConfig.discord.discordDmVerificationMessage
                    .replace("{CODE}", code)
                    .replace("{PLAYER}", player.getName())
            ))
            .doOnSuccess(msg -> {
                this.noticeService.player(playerUuid, messages -> messages.discord.verificationCodeSent);
                this.showVerificationDialog(player);
            })
            .doOnError(error -> {
                this.verificationCache.invalidate(playerUuid);
                this.noticeService.player(playerUuid, messages -> messages.discord.cannotSendDm);
            })
            .then();
    }

    private void showVerificationDialog(Player player) {
        Dialog dialog = this.dialogFactory.create(
            (view, enteredCode) -> this.handleVerification(player, enteredCode),
            () -> this.handleCancellation(player)
        );
        player.showDialog(dialog);
    }

    private void handleVerification(Player player, String enteredCode) {
        UUID playerUuid = player.getUniqueId();

        this.verificationCache.get(playerUuid).ifPresentOrElse(
            verificationData -> {
                if (!verificationData.code().equals(enteredCode)) {
                    this.verificationCache.invalidate(playerUuid);
                    this.noticeService.player(playerUuid, messages -> messages.discord.invalidCode);
                    return;
                }

                this.verificationCache.invalidate(playerUuid);
                this.discordLinkService.createLink(playerUuid, verificationData.discordId())
                    .thenAccept(success -> {
                        if (success) {
                            this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
                            return;
                        }
                        this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
                    });
            },
            () -> this.noticeService.player(playerUuid, messages -> messages.discord.verificationExpired)
        );
    }

    private void handleCancellation(Player player) {
        UUID playerUuid = player.getUniqueId();
        this.verificationCache.invalidate(playerUuid);
        this.noticeService.player(playerUuid, messages -> messages.discord.verificationCancelled);
    }
}
