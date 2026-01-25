package com.eternalcode.parcellockers.discord.notification;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import reactor.core.scheduler.Schedulers;

/**
 * Discord4J-based implementation of DiscordNotificationService.
 * Uses the Discord4J library to send notifications.
 */
public class Discord4JNotificationService implements DiscordNotificationService {

    private final GatewayDiscordClient client;
    private final Logger logger;

    public Discord4JNotificationService(GatewayDiscordClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public void sendPrivateMessage(long discordId, String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        this.client.getUserById(Snowflake.of(discordId))
            .flatMap(user -> user.getPrivateChannel())
            .flatMap(channel -> channel.createMessage(message))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(msg -> future.complete(true))
            .doOnError(error -> {
                this.logger.warning("Failed to send private message to Discord user " + discordId + ": " + error.getMessage());
                future.complete(false);
            })
            .subscribe();
    }
}
