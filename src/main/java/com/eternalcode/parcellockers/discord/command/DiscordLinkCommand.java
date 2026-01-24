package com.eternalcode.parcellockers.discord.command;

import com.eternalcode.multification.notice.provider.NoticeProvider;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import reactor.core.publisher.Mono;

/**
 * Command responsible for linking a Minecraft account with a Discord account.
 * <p>
 * This implementation relies on Paper's Dialog API ({@code io.papermc.paper.dialog}),
 * which is marked as unstable and may change or be removed in future Paper versions.
 * If the Dialog API becomes unavailable, this command may need to be updated or
 * replaced with a more stable flow (for example, chat-based verification).
 */
@Command(name = "parcel linkdiscord")
public class DiscordLinkCommand {

    private static final Random RANDOM = new Random();

    private final GatewayDiscordClient client;
    private final DiscordLinkService discordLinkService;
    private final NoticeService noticeService;
    private final MiniMessage miniMessage;
    private final MessageConfig messageConfig;

    private final Cache<UUID, VerificationData> authCodesCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();

    public DiscordLinkCommand(
        GatewayDiscordClient client,
        DiscordLinkService discordLinkService,
        NoticeService noticeService,
        MiniMessage miniMessage,
        MessageConfig messageConfig
    ) {
        this.client = client;
        this.discordLinkService = discordLinkService;
        this.noticeService = noticeService;
        this.miniMessage = miniMessage;
        this.messageConfig = messageConfig;
    }

    @Execute
    void linkSelf(@Context Player player, @Arg long discordId) {
        UUID playerUuid = player.getUniqueId();
        String discordIdString = String.valueOf(discordId);

        if (this.authCodesCache.getIfPresent(playerUuid) != null) {
            this.noticeService.player(playerUuid, messages -> messages.discord.verificationAlreadyPending);
            return;
        }

        this.validateAndLink(playerUuid, discordIdString)
            .thenCompose(validationResult -> {
                if (!validationResult.isValid()) {
                    this.noticeService.player(playerUuid, validationResult.errorMessage());
                    return CompletableFuture.completedFuture(null);
                }

                return this.sendVerification(playerUuid, discordIdString, player, validationResult.discordUser())
                    .toFuture();
            })
            .exceptionally(error -> {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
                return null;
            });
    }

    @Execute
    @Permission("parcellockers.admin")
    void linkOther(@Context CommandSender sender, @Arg String playerName, @Arg long discordId) {
        String discordIdString = String.valueOf(discordId);

        this.resolvePlayerUuid(playerName)
            .thenCompose(playerUuid -> {
                if (playerUuid == null) {
                    this.noticeService.viewer(sender, messages -> messages.discord.userNotFound);
                    return CompletableFuture.completedFuture(null);
                }

                return this.validateAndLink(playerUuid, discordIdString)
                    .thenCompose(validationResult -> {
                        if (!validationResult.isValid()) {
                            this.noticeService.viewer(sender, validationResult.errorMessage());
                            return CompletableFuture.completedFuture(null);
                        }

                        return this.discordLinkService.createLink(playerUuid, discordIdString)
                            .thenAccept(success -> {
                                if (success) {
                                    this.noticeService.viewer(sender, messages -> messages.discord.adminLinkSuccess);
                                    this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
                                } else {
                                    this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
                                }
                            });
                    });
            })
            .exceptionally(error -> {
                this.noticeService.viewer(sender, messages -> messages.discord.linkFailed);
                return null;
            });
    }

    private Mono<Void> sendVerification(UUID playerUuid, String discordId, Player player, User discordUser) {
        String code = this.generateVerificationCode();

        VerificationData data = new VerificationData(discordId, code);
        if (this.authCodesCache.asMap().putIfAbsent(playerUuid, data) != null) {
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
                this.authCodesCache.invalidate(playerUuid);
                this.noticeService.player(playerUuid, messages -> messages.discord.cannotSendDm);
            })
            .then();
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
            // Invalidate the verification code after a failed attempt to prevent repeated guessing
            this.authCodesCache.invalidate(playerUuid);
            this.noticeService.player(playerUuid, messages -> messages.discord.invalidCode);
            return;
        }

        // Code matches - remove from cache and create the link
        this.authCodesCache.invalidate(playerUuid);

        this.discordLinkService.createLink(playerUuid, verificationData.discordId()).thenAccept(success -> {
            if (success) {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkSuccess);
            } else {
                this.noticeService.player(playerUuid, messages -> messages.discord.linkFailed);
            }
        });
    }

    private CompletableFuture<ValidationResult> validateAndLink(UUID playerUuid, String discordIdString) {
        return this.discordLinkService.findLinkByPlayer(playerUuid)
            .thenCompose(existingPlayerLink -> {
                if (existingPlayerLink.isPresent()) {
                    return CompletableFuture.completedFuture(
                        ValidationResult.error(messages -> messages.discord.alreadyLinked)
                    );
                }

                return this.discordLinkService.findLinkByDiscordId(discordIdString)
                    .thenCompose(existingDiscordLink -> {
                        if (existingDiscordLink.isPresent()) {
                            return CompletableFuture.completedFuture(
                                ValidationResult.error(messages -> messages.discord.discordAlreadyLinked)
                            );
                        }

                        final long discordIdLong;
                        try {
                            discordIdLong = Long.parseLong(discordIdString);
                        } catch (NumberFormatException e) {
                            return CompletableFuture.completedFuture(
                                ValidationResult.error(messages -> messages.discord.userNotFound)
                            );
                        }

                        return this.client.getUserById(Snowflake.of(discordIdLong))
                            .map(ValidationResult::success)
                            .onErrorResume(error -> Mono.just(
                                ValidationResult.error(messages -> messages.discord.userNotFound)
                            ))
                            .toFuture();
                    });
            });
    }

    private String generateVerificationCode() {
        int code = ThreadLocalRandom.current().nextInt(1000, 10000);
        return String.valueOf(code);
    }

    private CompletableFuture<UUID> resolvePlayerUuid(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Player online = Bukkit.getPlayerExact(playerName);
            if (online != null) {
                return online.getUniqueId();
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
        });
    }

    private record VerificationData(String discordId, String code) {}

    private record ValidationResult(
        boolean valid,
        User discordUser,
        NoticeProvider<MessageConfig> errorMessageGetter
    ) {
        static ValidationResult success(User user) {
            return new ValidationResult(true, user, null);
        }

        static ValidationResult error(NoticeProvider<MessageConfig> messageGetter) {
            return new ValidationResult(false, null, messageGetter);
        }

        boolean isValid() {
            return this.valid;
        }

        NoticeProvider<MessageConfig> errorMessage() {
            return this.errorMessageGetter;
        }
    }
}

