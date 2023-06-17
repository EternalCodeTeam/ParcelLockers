package com.eternalcode.parcellockers.user;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class User {

    private final String name;
    private final Set<UUID> parcels;
    private final UUID uuid;

    public User(Player player, Set<UUID> parcels) {
        this.name = player.getName();
        this.parcels = parcels;
        this.uuid = player.getUniqueId();
    }

    protected User(UUID uuid, String name, Set<UUID> parcels) {
        this.name = name;
        this.uuid = uuid;
        this.parcels = parcels;
    }

    public String getName() {
        return this.name;
    }

    public Set<UUID> getParcels() {
        return Collections.unmodifiableSet(this.parcels);
    }

    public void addParcel(UUID parcel) {
        this.parcels.add(parcel);
    }

    public void removeParcel(UUID parcel) {
        this.parcels.remove(parcel);
    }

    public UUID getUuid() {
        return this.uuid;
    }
}