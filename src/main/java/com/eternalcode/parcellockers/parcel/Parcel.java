package com.eternalcode.parcellockers.parcel;

import dev.rollczi.litecommands.shared.Preconditions;

import java.util.Set;
import java.util.UUID;


public record Parcel(UUID uuid,
                     UUID sender,
                     String name,
                     String description,
                     boolean priority,
                     Set<UUID> recipients,
                     UUID receiver,
                     ParcelSize size,
                     UUID entryLocker,
                     UUID destinationLocker) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private UUID uuid;
        private UUID sender;
        private String name;
        private String description;
        private boolean priority;
        private Set<UUID> recipients;
        private UUID receiver;
        private ParcelSize size;
        private UUID entryLocker;
        private UUID destinationLocker;

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder sender(UUID sender) {
            this.sender = sender;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder priority(boolean priority) {
            this.priority = priority;
            return this;
        }

        public Builder recipients(Set<UUID> recipients) {
            this.recipients = recipients;
            return this;
        }

        public Builder receiver(UUID receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder size(ParcelSize size) {
            this.size = size;
            return this;
        }

        public Builder entryLocker(UUID entryLocker) {
            this.entryLocker = entryLocker;
            return this;
        }

        public Builder destinationLocker(UUID destinationLocker) {
            this.destinationLocker = destinationLocker;
            return this;
        }


        public Parcel build() {
            Preconditions.notNull(this.uuid, "uuid");
            Preconditions.notNull(this.sender, "sender");
            Preconditions.notNull(this.name, "name");
            Preconditions.notNull(this.receiver, "receiver");
            Preconditions.notNull(this.size, "size");
            Preconditions.notNull(this.entryLocker, "entryLocker");
            Preconditions.notNull(this.destinationLocker, "destinationLocker");
            return new Parcel(this.uuid, this.sender, this.name, this.description, this.priority, this.recipients, this.receiver, this.size, this.entryLocker, this.destinationLocker);
        }
    }
}
