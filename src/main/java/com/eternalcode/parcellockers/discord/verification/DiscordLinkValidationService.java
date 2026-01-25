package com.eternalcode.parcellockers.discord.verification;

import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;

public class DiscordLinkValidationService {

    private final DiscordLinkService discordLinkService;
    private final GatewayDiscordClient discordClient;

    public DiscordLinkValidationService(
        DiscordLinkService discordLinkService,
        GatewayDiscordClient discordClient
    ) {
        this.discordLinkService = discordLinkService;
        this.discordClient = discordClient;
    }

    public CompletableFuture<ValidationResult> validate(UUID playerUuid, long discordId) {
        return this.discordLinkService.findLinkByPlayer(playerUuid).thenCompose(optionalLink -> {
            if (optionalLink.isPresent()) {
                return CompletableFuture.completedFuture(ValidationResult.invalid("alreadyLinked"));
            }

            return this.discordLinkService.findLinkByDiscordId(discordId).thenCompose(optionalDiscordLink -> {
                if (optionalDiscordLink.isPresent()) {
                    return CompletableFuture.completedFuture(ValidationResult.invalid("discordAlreadyLinked"));
                }

                return this.discordClient.getUserById(Snowflake.of(discordId))
                    .map(user -> ValidationResult.valid())
                    .onErrorResume(error -> Mono.just(ValidationResult.invalid("userNotFound")))
                    .toFuture();
            });
        });
    }

    public CompletableFuture<User> getDiscordUser(long discordId) {
        return this.discordClient.getUserById(Snowflake.of(discordId)).toFuture();
    }
}
