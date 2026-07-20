package com.eternalcode.parcellockers.discord.notification;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import java.util.logging.Logger;
import reactor.core.scheduler.Schedulers;

public class Discord4JNotificationService implements DiscordNotificationService {

    private final GatewayDiscordClient client;
    private final Logger logger;

    public Discord4JNotificationService(GatewayDiscordClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public void sendPrivateMessage(long discordId, String message) {
        this.client.getUserById(Snowflake.of(discordId))
            .flatMap(user -> user.getPrivateChannel())
            .flatMap(channel -> channel.createMessage(message))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(error -> this.logger.warning("Failed to send private message to Discord user " + discordId + ": " + error.getMessage()))
            .subscribe();
    }
}
