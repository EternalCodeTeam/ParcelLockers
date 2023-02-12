package com.eternalcode.parcellockers.parcel;

import java.util.UUID;


public class Parcel {

    private final UUID uuid;
    private final UUID sender;
    private final ParcelMeta meta;

    public Parcel(UUID uuid, UUID sender, ParcelMeta meta) {
        this.uuid = uuid;
        this.sender = sender;
        this.meta = meta;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public UUID getSender() {
        return this.sender;
    }

    public ParcelMeta getMeta() {
        return this.meta;
    }
}
