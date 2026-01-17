package com.eternalcode.parcellockers.discord.repository;

import com.eternalcode.parcellockers.discord.DiscordLink;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;

@DatabaseTable(tableName = "discord_links")
class DiscordLinkEntity {

    @DatabaseField(id = true, columnName = "minecraft_uuid")
    private String minecraftUuid;

    @DatabaseField(index = true, columnName = "discord_id")
    private String discordId;

    DiscordLinkEntity() {}

    DiscordLinkEntity(String minecraftUuid, String discordId) {
        this.minecraftUuid = minecraftUuid;
        this.discordId = discordId;
    }

    public static DiscordLinkEntity fromDomain(DiscordLink link) {
        return new DiscordLinkEntity(
            link.minecraftUuid().toString(),
            link.discordId()
        );
    }

    public DiscordLink toDomain() {
        return new DiscordLink(
            UUID.fromString(this.minecraftUuid),
            this.discordId
        );
    }

}
