package com.eternalcode.parcellockers.discord.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.discord.DiscordLink;
import com.j256.ormlite.stmt.DeleteBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordLinkRepositoryOrmLite extends AbstractRepositoryOrmLite implements DiscordLinkRepository {

    public DiscordLinkRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(DiscordLinkEntity.class);
    }

    @Override
    public CompletableFuture<Boolean> save(DiscordLink link) {
        return this.upsert(DiscordLinkEntity.class, DiscordLinkEntity.fromDomain(link))
            .thenApply(status -> status.isCreated() || status.isUpdated());
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findByPlayerUuid(UUID playerUuid) {
        return this.selectSafe(DiscordLinkEntity.class, playerUuid)
            .thenApply(optionalEntity -> optionalEntity.map(DiscordLinkEntity::toDomain));
    }

    @Override
    public CompletableFuture<Optional<DiscordLink>> findByDiscordId(long discordId) {
        return this.action(DiscordLinkEntity.class, dao -> dao.queryBuilder()
            .where()
            .eq(DiscordLinkEntity.ID_COLUMN_NAME, discordId)
            .queryForFirst())
            .thenApply(entity -> Optional.ofNullable(entity).map(DiscordLinkEntity::toDomain));
    }

    @Override
    public CompletableFuture<Boolean> deleteByPlayerUuid(UUID playerUuid) {
        return this.deleteById(DiscordLinkEntity.class, playerUuid)
            .thenApply(deletedRows -> deletedRows > 0);
    }

    @Override
    public CompletableFuture<Boolean> deleteByDiscordId(long discordId) {
        return this.action(DiscordLinkEntity.class, dao -> {
            DeleteBuilder<DiscordLinkEntity, Object> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(DiscordLinkEntity.ID_COLUMN_NAME, discordId);
            return deleteBuilder.delete();
        }).thenApply(deletedRows -> deletedRows > 0);
    }
}
