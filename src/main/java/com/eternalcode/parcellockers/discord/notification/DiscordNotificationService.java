package com.eternalcode.parcellockers.discord.notification;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending Discord notifications.
 * Implementations can use different Discord libraries (Discord4J, DiscordSRV/JDA, etc.)
 */
public interface DiscordNotificationService {

    /**
     * Sends a private message to a Discord user.
     *
     * @param discordId the Discord user ID to send the message to
     * @param message the message content to send
     * @return a CompletableFuture that completes when the message is sent, returning true on success
     */
    CompletableFuture<Boolean> sendPrivateMessage(String discordId, String message);
}
