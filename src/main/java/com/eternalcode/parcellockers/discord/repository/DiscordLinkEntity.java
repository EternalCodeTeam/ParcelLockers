package com.eternalcode.parcellockers.discord.repository;

import com.eternalcode.parcellockers.discord.DiscordLink;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;

@DatabaseTable(tableName = "discord_links")
class DiscordLinkEntity {

    static final String ID_COLUMN_NAME = "discord_id";

    @DatabaseField(id = true, columnName = "minecraft_uuid")
    private UUID minecraftUuid;

    @DatabaseField(index = true, columnName = ID_COLUMN_NAME)
    private long discordId;

    DiscordLinkEntity() {}

    DiscordLinkEntity(UUID minecraftUuid, long discordId) {
        this.minecraftUuid = minecraftUuid;
        this.discordId = discordId;
    }

    public static DiscordLinkEntity fromDomain(DiscordLink link) {
        return new DiscordLinkEntity(
            link.minecraftUuid(),
            link.discordId()
        );
    }

    public DiscordLink toDomain() {
        return new DiscordLink(
            this.minecraftUuid,
            this.discordId
        );
    }

}
