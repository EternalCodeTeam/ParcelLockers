package com.eternalcode.parcellockers.discord.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.discord.DiscordLink;
import com.eternalcode.parcellockers.shared.exception.DatabaseException;
import com.j256.ormlite.dao.Dao.CreateOrUpdateStatus;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordLinkRepositoryOrmLite extends AbstractRepositoryOrmLite implements DiscordLinkRepository {

    public static final String ID_COLUMN_NAME = "discord_id";

    public DiscordLinkRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), DiscordLink.class);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize DiscordLink table", ex);
        }
    }

    @Override
    public CompletableFuture<Boolean> save(DiscordLink link) {
        return this.save(DiscordLinkEntity.class, DiscordLinkEntity.fromDomain(link))
            .thenApply(CreateOrUpdateStatus::isCreated);
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findByPlayerUuid(UUID playerUuid) {
        return this.selectSafe(DiscordLinkEntity.class, playerUuid.toString())
            .thenApply(optionalEntity -> optionalEntity.map(DiscordLinkEntity::toDomain));
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findByDiscordId(String discordId) {
        return this.action(DiscordLinkEntity.class, dao -> {
            var queryBuilder = dao.queryBuilder()
            .where().eq(ID_COLUMN_NAME, discordId);
            return dao.queryForFirst(queryBuilder.prepare());
        }).thenApply(optionalEntity -> optionalEntity != null ? Optional.of(optionalEntity.toDomain()) : Optional.empty());
    }

    @Override
    public CompletableFuture<Boolean> deleteByPlayerUuid(UUID playerUuid) {
        return this.deleteById(DiscordLinkEntity.class, playerUuid.toString())
            .thenApply(deletedRows -> deletedRows > 0);
    }

    @Override
    public CompletableFuture<Boolean> deleteByDiscordId(String discordId) {
        return this.action(DiscordLinkEntity.class, dao -> {
            var deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(ID_COLUMN_NAME, discordId);
            return deleteBuilder.delete();
        }).thenApply(deletedRows -> deletedRows > 0);
    }
}
