package com.eternalcode.parcellockers.parcel;

import java.util.UUID;


public record Parcel(UUID uuid, UUID sender, ParcelMeta meta) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private UUID uuid;
        private UUID sender;
        private ParcelMeta meta;

        public Builder() {
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder sender(UUID sender) {
            this.sender = sender;
            return this;
        }

        public Builder meta(ParcelMeta meta) {
            this.meta = meta;
            return this;
        }

        public Parcel build() {
            return new Parcel(this.uuid, this.sender, this.meta);
        }
    }
}
