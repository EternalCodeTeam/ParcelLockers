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
    private final UUID entryLocker;
    private final UUID destinationLocker;

    public ParcelMeta(String name, String description, boolean priority, UUID receiver, ParcelSize size, UUID entryLocker, UUID destinationLocker) {
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

    public UUID getEntryLocker() {
        return this.entryLocker;
    }

    public UUID getDestinationLocker() {
        return this.destinationLocker;
    }
}
