package com.eternalcode.parcellockers.discord;

import com.eternalcode.parcellockers.discord.repository.DiscordLinkRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordLinkServiceImpl implements DiscordLinkService {

    private final DiscordLinkRepository repository;

    public DiscordLinkServiceImpl(DiscordLinkRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByPlayer(UUID playerUuid) {
        return this.repository.findByPlayerUuid(playerUuid);
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(String discordId) {
        return this.repository.findByDiscordId(discordId);
    }

    @Override
    public CompletableFuture<Boolean> createLink(UUID playerUuid, String discordId) {
        DiscordLink link = new DiscordLink(playerUuid, discordId);
        return this.repository.save(link);
    }

    @Override
    public CompletableFuture<Boolean> unlinkPlayer(UUID playerUuid) {
        return this.repository.deleteByPlayerUuid(playerUuid);
    }

    @Override
    public CompletableFuture<Boolean> unlinkDiscordId(String discordId) {
        return this.repository.deleteByDiscordId(discordId);
    }
}
