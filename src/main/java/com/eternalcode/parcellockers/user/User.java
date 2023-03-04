package com.eternalcode.parcellockers.user;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class User {

    private final String name;
    private final Set<UUID> parcels;
    private final Instant lastLogin;
    private final UUID uuid;

    public User(Player player, Set<UUID> parcels, Instant lastLogin) {
        this.name = player.getName();
        this.parcels = parcels;
        this.lastLogin = lastLogin;
        this.uuid = player.getUniqueId();
    }

    protected User(UUID uuid, String name, Instant lastLogin, Set<UUID> parcels) {
        this.name = name;
        this.lastLogin = lastLogin;
        this.uuid = uuid;
        this.parcels = parcels;
    }

    public String getName() {
        return this.name;
    }

    public Set<UUID> getParcels() {
        return this.parcels;
    }

    public Instant getLastLogin() {
        return this.lastLogin;
    }

    public UUID getUuid() {
        return this.uuid;
    }
}
