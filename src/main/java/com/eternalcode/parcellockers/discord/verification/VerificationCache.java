package com.eternalcode.parcellockers.discord.verification;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

class VerificationCache {

    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(2);

    private final Cache<UUID, VerificationData> cache = Caffeine.newBuilder().expireAfterWrite(EXPIRATION_TIME).build();

    boolean hasPendingVerification(UUID playerUuid) {
        return this.cache.getIfPresent(playerUuid) != null;
    }

    Optional<VerificationData> get(UUID playerUuid) {
        return Optional.ofNullable(this.cache.getIfPresent(playerUuid));
    }

    boolean putIfAbsent(UUID playerUuid, VerificationData data) {
        return this.cache.asMap().putIfAbsent(playerUuid, data) == null;
    }

    void invalidate(UUID playerUuid) {
        this.cache.invalidate(playerUuid);
    }
}
