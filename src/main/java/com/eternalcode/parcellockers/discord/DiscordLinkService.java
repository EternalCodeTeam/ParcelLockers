package com.eternalcode.parcellockers.discord;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DiscordLinkService {

    CompletableFuture<UnlinkResult> unlinkDiscordId(long discordId);

    CompletableFuture<UnlinkResult> unlinkPlayer(UUID playerUuid);

    CompletableFuture<LinkResult> createLink(UUID playerUuid, long discordId);

    CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(long discordId);

    CompletableFuture<Optional<DiscordLink>> findLinkByPlayer(UUID playerUuid);
}
