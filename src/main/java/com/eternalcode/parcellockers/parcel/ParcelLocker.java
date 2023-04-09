package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.shared.Position;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParcelLocker {

    private final UUID uuid;
    private final String description;
    private final Set<Parcel> parcels;
    private final Position position;

    public ParcelLocker(UUID uuid, String description, Position position) {
        this.uuid = uuid;
        this.description = description;
        this.parcels = new HashSet<>();
        this.position = position;
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

    public Position getPosition() {
        return this.position;
    }

    public void addParcel(Parcel parcel) {
        this.parcels.add(parcel);
    }

    public void removeParcel(Parcel parcel) {
        this.parcels.remove(parcel);
    }

    public static class Builder {
        private UUID uuid;
        private String description;
        private Set<Parcel> parcels;
        private Position position;

        public Builder setUuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setPosition(Position position) {
            this.position = position;
            return this;
        }

        public ParcelLocker build() {
            return new ParcelLocker(uuid, description, position);
        }
    }
}
