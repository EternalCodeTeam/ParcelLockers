package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.discord.DiscordLink;
import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
@Command(name = "parcel linkdiscord")
public class DiscordLinkCommand {

    private static final Random RANDOM = new Random();

    private final GatewayDiscordClient client;
    private final DiscordLinkRepository discordLinkRepository;
    private final NoticeService noticeService;
    private final MiniMessage miniMessage;
    private final Scheduler scheduler;
    private final MessageConfig messageConfig;

    private final Cache<UUID, VerificationData> authCodesCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();

    public DiscordLinkCommand(
        GatewayDiscordClient client,
        DiscordLinkRepository discordLinkRepository,
        NoticeService noticeService,
        MiniMessage miniMessage,
        Scheduler scheduler,
        MessageConfig messageConfig
    ) {
        this.client = client;
        this.discordLinkRepository = discordLinkRepository;
        this.noticeService = noticeService;
        this.miniMessage = miniMessage;
        this.scheduler = scheduler;
        this.messageConfig = messageConfig;
    }

    @Execute
    void linkSelf(@Context Player player, @Arg long discordId) {
        UUID playerUuid = player.getUniqueId();
        String discordIdString = String.valueOf(discordId);

        // Check if the player already has a pending verification
        if (this.authCodesCache.getIfPresent(playerUuid) != null) {
            this.noticeService.player(playerUuid, messages -> messages.discord.verificationAlreadyPending);
            return;
        }

        // Check if the player already has a linked Discord account
        this.discordLinkRepository.findByPlayerUuid(playerUuid).thenAccept(existingLink -> {
            if (existingLink.isPresent()) {
                this.noticeService.player(playerUuid, messages -> messages.discord.alreadyLinked);
                return;
            }

            // Check if the Discord account is already linked to another Minecraft account
            this.discordLinkRepository.findByDiscordId(discordIdString).thenAccept(existingDiscordLink -> {
                if (existingDiscordLink.isPresent()) {
                    this.noticeService.player(playerUuid, messages -> messages.discord.discordAlreadyLinked);
                    return;
                }

                // Try to get the Discord user
                User discordUser;
                try {
                    discordUser = this.client.getUserById(Snowflake.of(discordId)).block();
                } catch (Exception ex) {
                    this.noticeService.player(playerUuid, messages -> messages.discord.userNotFound);
                    return;
                }

                if (discordUser == null) {
                    this.noticeService.player(playerUuid, messages -> messages.discord.userNotFound);
                    return;
                }

                // Generate a 4-digit verification code
                String verificationCode = this.generateVerificationCode();

                // Store the verification data in cache
                this.authCodesCache.put(playerUuid, new VerificationData(discordIdString, verificationCode));

                // Send the verification code to the Discord user via DM
                discordUser.getPrivateChannel()
                    .flatMap(channel -> channel.createMessage(
                        this.messageConfig.discord.discordDmVerificationMessage
                            .replace("{CODE}", verificationCode)
                            .replace("{PLAYER}", player.getName())
                    ))
                    .doOnSuccess(message -> {
                        this.noticeService.player(playerUuid, messages -> messages.discord.verificationCodeSent);
                        this.scheduler.run(() -> this.showVerificationDialog(player));
                    })
                    .doOnError(error -> {
                        this.authCodesCache.invalidate(playerUuid);
                        this.noticeService.player(playerUuid, messages -> messages.discord.cannotSendDm);
                    })
                    .subscribe();
            });
        });
    }

    @Execute
    @Permission("parcellockers.admin")
    void linkOther(@Context CommandSender sender, @Arg Player player, @Arg long discordId) {
        UUID playerUuid = player.getUniqueId();
        String discordIdString = String.valueOf(discordId);

        // Admin bypass - directly link without verification
        this.discordLinkRepository.findByPlayerUuid(playerUuid).thenAccept(existingLink -> {
            if (existingLink.isPresent()) {
                this.noticeService.console(messages -> messages.discord.playerAlreadyLinked);
                return;
            }

            this.discordLinkRepository.findByDiscordId(discordIdString).thenAccept(existingDiscordLink -> {
                if (existingDiscordLink.isPresent()) {
                    this.noticeService.console(messages -> messages.discord.discordAlreadyLinked);
                    return;
                }

                DiscordLink link = new DiscordLink(playerUuid, discordIdString);
                this.discordLinkRepository.save(link).thenAccept(success -> {
                    if (success) {
                        this.noticeService.viewer(sender, messages -> messages.discord.adminLinkSuccess);
                        this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
                    } else {
                        this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
                    }
                });
            });
        });
    }

    private void showVerificationDialog(Player player) {
        Dialog verificationDialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(this.miniMessage.deserialize(this.messageConfig.discord.verificationDialogTitle))
                .canCloseWithEscape(false)
                .inputs(List.of(
                    DialogInput.text("code", this.miniMessage.deserialize(this.messageConfig.discord.verificationDialogPlaceholder))
                        .build()
                ))
                .build()
            )
            .type(DialogType.confirmation(
                ActionButton.create(
                    this.miniMessage.deserialize("<dark_green>Verify"),
                    this.miniMessage.deserialize("<green>Click to verify your Discord account"),
                    200,
                    DialogAction.customClick((DialogResponseView view, Audience audience) -> {
                        String enteredCode = view.getText("code");
                        this.handleVerification(player, enteredCode);
                    }, ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                        .build())
                ),
                ActionButton.create(
                    this.miniMessage.deserialize("<dark_red>Cancel"),
                    this.miniMessage.deserialize("<red>Click to cancel verification"),
                    200,
                    DialogAction.customClick(
                        (DialogResponseView view, Audience audience) -> {
                            this.authCodesCache.invalidate(player.getUniqueId());
                            this.noticeService.player(player.getUniqueId(), messages -> messages.discord.verificationCancelled);
                        },
                        ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build())
                )
            ))
        );

        player.showDialog(verificationDialog);
    }

    private void handleVerification(Player player, String enteredCode) {
        UUID playerUuid = player.getUniqueId();
        VerificationData verificationData = this.authCodesCache.getIfPresent(playerUuid);

        if (verificationData == null) {
            this.noticeService.player(playerUuid, messages -> messages.discord.verificationExpired);
            return;
        }

        if (!verificationData.code().equals(enteredCode)) {
            this.noticeService.player(playerUuid, messages -> messages.discord.invalidCode);
            return;
        }

        // Code matches - remove from cache and create the link
        this.authCodesCache.invalidate(playerUuid);

        DiscordLink link = new DiscordLink(playerUuid, verificationData.discordId());
        this.discordLinkRepository.save(link).thenAccept(success -> {
            if (success) {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
            } else {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
            }
        });
    }

    private String generateVerificationCode() {
        int code = 1000 + RANDOM.nextInt(9000); // generates 1000-9999
        return String.valueOf(code);
    }

    private record VerificationData(String discordId, String code) {}
}
