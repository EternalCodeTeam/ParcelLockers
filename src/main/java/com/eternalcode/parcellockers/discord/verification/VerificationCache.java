package com.eternalcode.parcellockers.discord.verification;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Cache for storing pending Discord verification requests.
 */
public class VerificationCache {

    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(2);

    private final Cache<UUID, VerificationData> cache = Caffeine.newBuilder()
        .expireAfterWrite(EXPIRATION_TIME)
        .build();

    /**
     * Checks if a player has a pending verification.
     *
     * @param playerUuid the player's UUID
     * @return true if a pending verification exists
     */
    public boolean hasPendingVerification(UUID playerUuid) {
        return this.cache.getIfPresent(playerUuid) != null;
    }

    /**
     * Retrieves the pending verification data for a player.
     *
     * @param playerUuid the player's UUID
     * @return an Optional containing the verification data, or empty if none exists
     */
    public Optional<VerificationData> get(UUID playerUuid) {
        return Optional.ofNullable(this.cache.getIfPresent(playerUuid));
    }

    /**
     * Stores verification data for a player if no pending verification exists.
     *
     * @param playerUuid the player's UUID
     * @param data the verification data to store
     * @return true if the data was stored, false if a pending verification already exists
     */
    public boolean putIfAbsent(UUID playerUuid, VerificationData data) {
        return this.cache.asMap().putIfAbsent(playerUuid, data) == null;
    }

    /**
     * Removes the pending verification for a player.
     *
     * @param playerUuid the player's UUID
     */
    public void invalidate(UUID playerUuid) {
        this.cache.invalidate(playerUuid);
    }
}
