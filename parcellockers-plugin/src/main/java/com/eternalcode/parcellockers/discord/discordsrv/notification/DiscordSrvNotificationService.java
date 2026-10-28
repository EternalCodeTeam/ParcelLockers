package com.eternalcode.parcellockers.discord.discordsrv.notification;

import com.eternalcode.parcellockers.discord.notification.DiscordNotificationService;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DiscordSrvNotificationService implements DiscordNotificationService {

    private final Logger logger;

    public DiscordSrvNotificationService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void sendPrivateMessage(long discordId, String message) {
        CompletableFuture.supplyAsync(() -> {
            try {
                User user = DiscordUtil.getUserById(Long.toString(discordId));
                if (user == null) {
                    this.logger.warning("Could not find Discord user with ID: " + discordId);
                    return false;
                }

                user.openPrivateChannel().flatMap(channel -> channel.sendMessage(message)).queue(
                    success -> {},
                    error -> this.logger.warning(
                        "Failed to send private message to Discord user " + discordId + ": " + error.getMessage())
                );

                return true;
            } catch (Exception e) {
                this.logger.warning("Failed to send private message to Discord user " + discordId + ": " + e.getMessage());
                return false;
            }
        });
    }
}
