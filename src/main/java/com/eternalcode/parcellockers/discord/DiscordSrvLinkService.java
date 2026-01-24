package com.eternalcode.parcellockers.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DiscordSRV-based implementation of DiscordLinkService.
 * Delegates account linking functionality to DiscordSRV plugin.
 */
public class DiscordSrvLinkService implements DiscordLinkService {

    private final Logger logger;

    public DiscordSrvLinkService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
            if (discordId == null) {
                return Optional.empty();
            }
            return Optional.of(new DiscordLink(playerUuid, discordId));
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);
            if (playerUuid == null) {
                return Optional.empty();
            }
            return Optional.of(new DiscordLink(playerUuid, discordId));
        });
    }

    @Override
    public CompletableFuture<Boolean> createLink(UUID playerUuid, String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DiscordSRV.getPlugin().getAccountLinkManager().link(discordId, playerUuid);
                return true;
            } catch (Exception e) {
                this.logger.log(Level.WARNING, "Failed to create DiscordSRV link", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unlinkPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DiscordSRV.getPlugin().getAccountLinkManager().unlink(playerUuid);
                return true;
            } catch (Exception exception) {
                this.logger.log(Level.WARNING, "Failed to unlink DiscordSRV player", exception);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unlinkDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);
                if (playerUuid == null) {
                    return false;
                }
                DiscordSRV.getPlugin().getAccountLinkManager().unlink(playerUuid);
                return true;
            } catch (Exception exception) {
                this.logger.log(Level.WARNING, "Failed to unlink DiscordSRV user by Discord ID", exception);
                return false;
            }
        });
    }

    /**
     * Gets the Discord user by their ID using DiscordSRV's JDA instance.
     */
    public Optional<User> getDiscordUser(String discordId) {
        try {
            return Optional.ofNullable(DiscordUtil.getUserById(discordId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the linking code for a player to use in Discord.
     * Returns empty if the player is already linked.
     */
    public Optional<String> getLinkingCode(UUID playerUuid) {
        String existingDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
        if (existingDiscordId != null) {
            return Optional.empty(); // Already linked
        }

        String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(playerUuid);
        return Optional.ofNullable(code);
    }
}
