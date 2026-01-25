package com.eternalcode.parcellockers.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import java.util.logging.Level;
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
        try {
            GatewayDiscordClient discordClient = DiscordClient.create(this.token)
                .login()
                .block();
            if (discordClient != null) {
                this.client = discordClient;
                this.logger.info("Successfully logged in to Discord.");
            } else {
                this.logger.severe("Failed to log in to Discord: login returned null client.");
            }
        } catch (Exception exception) {
            this.logger.log(Level.SEVERE, "Failed to log in to Discord", exception);
        }
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
