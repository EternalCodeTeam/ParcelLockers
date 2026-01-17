package com.eternalcode.parcellockers.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import java.util.logging.Logger;

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
        this.client = DiscordClient.create(this.token)
            .login()
            .block();
        this.logger.info("Successfully logged in to Discord.");
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
