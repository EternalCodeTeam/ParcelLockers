package com.eternalcode.parcellockers.discord.repository;

import com.eternalcode.parcellockers.discord.DiscordLink;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DiscordLinkRepository {

    CompletableFuture<Boolean> save(DiscordLink link);

    CompletableFuture<Optional<DiscordLink>> findByPlayerUuid(UUID playerUuid);
    CompletableFuture<Optional<DiscordLink>> findByDiscordId(String discordId);

    CompletableFuture<Boolean> deleteByPlayerUuid(UUID playerUuid);
    CompletableFuture<Boolean> deleteByDiscordId(String discordId);
}