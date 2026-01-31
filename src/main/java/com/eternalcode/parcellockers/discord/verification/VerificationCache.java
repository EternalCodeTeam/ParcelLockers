package com.eternalcode.parcellockers.discord.verification;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.UUID;

class VerificationCache {

    private final Cache<UUID, VerificationData> cache;

    VerificationCache(PluginConfig.DiscordSettings config) {
        this.cache = Caffeine.newBuilder().expireAfterWrite(config.linkCodeExpiration).build();
    }

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
