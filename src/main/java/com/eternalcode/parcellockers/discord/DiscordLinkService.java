package com.eternalcode.parcellockers.discord;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DiscordLinkService {

    CompletableFuture<Boolean> unlinkDiscordId(long discordId);

    CompletableFuture<Boolean> unlinkPlayer(UUID playerUuid);

    CompletableFuture<Boolean> createLink(UUID playerUuid, long discordId);

    CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(long discordId);

    CompletableFuture<Optional<DiscordLink>> findLinkByPlayer(UUID playerUuid);
}
