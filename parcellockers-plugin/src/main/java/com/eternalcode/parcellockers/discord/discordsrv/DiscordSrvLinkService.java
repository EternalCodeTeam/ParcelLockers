package com.eternalcode.parcellockers.discord.discordsrv;

import com.eternalcode.parcellockers.discord.DiscordLink;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.discord.LinkResult;
import com.eternalcode.parcellockers.discord.UnlinkResult;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            return Optional.of(new DiscordLink(playerUuid, Long.parseLong(discordId)));
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findLinkByDiscordId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(Long.toString(discordId));
            if (playerUuid == null) {
                return Optional.empty();
            }
            return Optional.of(new DiscordLink(playerUuid, discordId));
        });
    }

    @Override
    public CompletableFuture<LinkResult> createLink(UUID playerUuid, long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DiscordSRV.getPlugin().getAccountLinkManager().link(Long.toString(discordId), playerUuid);
                return LinkResult.SUCCESS;
            } catch (Exception e) {
                this.logger.log(Level.WARNING, "Failed to create DiscordSRV link", e);
                return LinkResult.GENERIC_FAILURE;
            }
        });
    }

    @Override
    public CompletableFuture<UnlinkResult> unlinkPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String existingDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
                if (existingDiscordId == null) {
                    return UnlinkResult.NOT_LINKED;
                }
                DiscordSRV.getPlugin().getAccountLinkManager().unlink(playerUuid);
                return UnlinkResult.SUCCESS;
            } catch (Exception exception) {
                this.logger.log(Level.WARNING, "Failed to unlink DiscordSRV player", exception);
                return UnlinkResult.GENERIC_FAILURE;
            }
        });
    }

    @Override
    public CompletableFuture<UnlinkResult> unlinkDiscordId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(Long.toString(discordId));
                if (playerUuid == null) {
                    return UnlinkResult.NOT_LINKED;
                }
                DiscordSRV.getPlugin().getAccountLinkManager().unlink(playerUuid);
                return UnlinkResult.SUCCESS;
            } catch (Exception exception) {
                this.logger.log(Level.WARNING, "Failed to unlink DiscordSRV user by Discord ID", exception);
                return UnlinkResult.GENERIC_FAILURE;
            }
        });
    }

    public Optional<User> getDiscordUser(String discordId) {
        try {
            return Optional.ofNullable(DiscordUtil.getUserById(discordId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> getLinkingCode(UUID playerUuid) {
        String existingDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
        if (existingDiscordId != null) {
            return Optional.empty(); // Already linked
        }

        String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(playerUuid);
        return Optional.ofNullable(code);
    }
}
