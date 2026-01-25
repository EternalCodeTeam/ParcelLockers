package com.eternalcode.parcellockers.discord.notification;

/**
 * Service interface for sending Discord notifications.
 * Implementations can use different Discord libraries (Discord4J, DiscordSRV/JDA, etc.)
 */
public interface DiscordNotificationService {

    /**
     * Sends a private message to a Discord user.
     *
     * @param discordId the Discord user ID to send the message to
     * @param message   the message content to send
     */
    void sendPrivateMessage(long discordId, String message);
}
