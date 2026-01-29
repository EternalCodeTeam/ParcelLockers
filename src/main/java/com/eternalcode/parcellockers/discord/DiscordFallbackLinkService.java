package com.eternalcode.parcellockers.discord;

import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordFallbackLinkService implements DiscordLinkService {

    private final DiscordLinkRepository repository;

    public DiscordFallbackLinkService(DiscordLinkRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByPlayer(UUID playerUuid) {
        return this.repository.findByPlayerUuid(playerUuid);
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(long discordId) {
        return this.repository.findByDiscordId(discordId);
    }

    @Override
    public CompletableFuture<LinkResult> createLink(UUID playerUuid, long discordId) {
        DiscordLink link = new DiscordLink(playerUuid, discordId);
        return this.repository.save(link).thenApply(success ->
            success ? LinkResult.SUCCESS : LinkResult.GENERIC_FAILURE
        );
    }

    @Override
    public CompletableFuture<UnlinkResult> unlinkPlayer(UUID playerUuid) {
        return this.repository.deleteByPlayerUuid(playerUuid).thenApply(success ->
            success ? UnlinkResult.SUCCESS : UnlinkResult.NOT_LINKED
        );
    }

    @Override
    public CompletableFuture<UnlinkResult> unlinkDiscordId(long discordId) {
        return this.repository.deleteByDiscordId(discordId).thenApply(success ->
            success ? UnlinkResult.SUCCESS : UnlinkResult.NOT_LINKED
        );
    }
}
