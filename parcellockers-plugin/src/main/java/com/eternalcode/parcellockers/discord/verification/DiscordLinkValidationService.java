package com.eternalcode.parcellockers.discord.verification;

import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.discord.LinkResult;
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

    public CompletableFuture<LinkResult> validate(UUID playerUuid, long discordId) {
        return this.discordLinkService.findLinkByPlayer(playerUuid).thenCompose(optionalLink -> {
            if (optionalLink.isPresent()) {
                return CompletableFuture.completedFuture(LinkResult.PLAYER_ALREADY_LINKED);
            }

            return this.discordLinkService.findLinkByDiscordId(discordId).thenCompose(optionalDiscordLink -> {
                if (optionalDiscordLink.isPresent()) {
                    return CompletableFuture.completedFuture(LinkResult.DISCORD_ALREADY_LINKED);
                }

                return this.discordClient.getUserById(Snowflake.of(discordId))
                    .map(user -> LinkResult.SUCCESS)
                    .onErrorResume(error -> Mono.just(LinkResult.DISCORD_USER_NOT_FOUND))
                    .toFuture();
            });
        });
    }

    public CompletableFuture<User> getDiscordUser(long discordId) {
        return this.discordClient.getUserById(Snowflake.of(discordId)).toFuture();
    }
}
