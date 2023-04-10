package com.eternalcode.parcellockers.parcel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParcelMeta {

    private final String name;
    private final String description;
    private final boolean priority;
    private final Set<UUID> recipients;
    private final UUID receiver;
    private final ParcelSize size;
    private final ParcelLocker entryLocker;
    private final ParcelLocker destinationLocker;

    public ParcelMeta(String name, String description, boolean priority, UUID receiver, ParcelSize size, ParcelLocker entryLocker, ParcelLocker destinationLocker) {
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.recipients = new HashSet<>();
        this.receiver = receiver;
        this.size = size;
        this.entryLocker = entryLocker;
        this.destinationLocker = destinationLocker;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isPriority() {
        return this.priority;
    }

    public Set<UUID> getRecipients() {
        return this.recipients;
    }

    public UUID getReceiver() {
        return this.receiver;
    }

    public ParcelSize getSize() {
        return this.size;
    }

    public ParcelLocker getEntryLocker() {
        return this.entryLocker;
    }

    public ParcelLocker getDestinationLocker() {
        return this.destinationLocker;
    }

    public static class Builder {

        private String name;
        private String description;
        private boolean priority;
        private UUID receiver;
        private ParcelSize size;
        private ParcelLocker entryLocker;
        private ParcelLocker destinationLocker;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setPriority(boolean priority) {
            this.priority = priority;
            return this;
        }

        public Builder setReceiver(UUID receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder setSize(ParcelSize size) {
            this.size = size;
            return this;
        }

        public Builder setEntryLocker(ParcelLocker entryLocker) {
            this.entryLocker = entryLocker;
            return this;
        }

        public Builder setDestinationLocker(ParcelLocker destinationLocker) {
            this.destinationLocker = destinationLocker;
            return this;
        }

        public ParcelMeta build() {
            return new ParcelMeta(this.name, this.description, this.priority, this.receiver, this.size, this.entryLocker, this.destinationLocker);
        }
    }
}
