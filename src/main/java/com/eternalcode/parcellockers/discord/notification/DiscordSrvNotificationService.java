package com.eternalcode.parcellockers.discord.notification;

import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * DiscordSRV-based implementation of DiscordNotificationService.
 * Uses DiscordSRV's JDA instance to send notifications.
 */
public class DiscordSrvNotificationService implements DiscordNotificationService {

    private final Logger logger;

    public DiscordSrvNotificationService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Boolean> sendPrivateMessage(String discordId, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = DiscordUtil.getUserById(discordId);
                if (user == null) {
                    this.logger.warning("Could not find Discord user with ID: " + discordId);
                    return false;
                }

                user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(message))
                    .queue(
                        success -> {},
                        error -> this.logger.warning("Failed to send private message to Discord user " + discordId + ": " + error.getMessage())
                    );

                return true;
            } catch (Exception e) {
                this.logger.warning("Failed to send private message to Discord user " + discordId + ": " + e.getMessage());
                return false;
            }
        });
    }
}
