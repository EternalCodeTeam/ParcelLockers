package com.eternalcode.parcellockers.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import java.util.logging.Logger;
import reactor.core.scheduler.Schedulers;

public class DiscordClientManager {

    private final String token;
    private final Logger logger;

    private GatewayDiscordClient client;

    public DiscordClientManager(String token, Logger logger) {
        this.token = token;
        this.logger = logger;
    }

    public void initialize() {
        this.logger.info("Discord integration is enabled. Logging in to Discord...");
        DiscordClient.create(this.token)
            .login()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(client -> {
                this.client = client;
                this.logger.info("Successfully logged in to Discord.");
            })
            .doOnError(error -> {
                this.logger.severe("Failed to log in to Discord: " + error.getMessage());
                error.printStackTrace();
            })
            .subscribe();
    }

    public void shutdown() {
        this.logger.info("Shutting down Discord client...");
        if (this.client != null) {
            this.client.logout().block();
        }
    }

    public GatewayDiscordClient getClient() {
        return this.client;
    }
}
