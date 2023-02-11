package com.eternalcode.parcellockers.parcel;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParcelLocker {

    private final UUID uuid;
    private final String description;
    private final Set<Parcel> parcels;
    private final Location location;

    public ParcelLocker(UUID uuid, String description, Location location) {
        this.uuid = uuid;
        this.description = description;
        this.parcels = new HashSet<>();
        this.location = location;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<Parcel> getParcels() {
        return this.parcels;
    }

    public Location getLocation() {
        return this.location;
    }
}
