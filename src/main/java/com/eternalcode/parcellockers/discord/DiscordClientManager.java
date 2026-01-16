package com.eternalcode.parcellockers.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import java.util.logging.Logger;

public class DiscordClientManager {

    private final String token;
    private final Snowflake serverId;
    private final Snowflake channelId;
    private final Snowflake botAdminRole;
    private final Logger logger;

    private GatewayDiscordClient client;

    public DiscordClientManager(String token, String serverId, String channelId, String botAdminRole, Logger logger) {
        this.token = token;
        this.serverId = Snowflake.of(serverId);
        this.channelId = Snowflake.of(channelId);
        this.botAdminRole = Snowflake.of(botAdminRole);
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
}
