package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.parcel.ParcelSize;

import java.util.UUID;

public class ParcelSendingGUIState {

    private String parcelName;
    private String parcelDescription;
    private ParcelSize size;
    private UUID receiver;
    private boolean priority;
    private UUID entryLocker;
    private UUID destinationLocker;

    public ParcelSendingGUIState() {
        this.parcelName = null;
        this.parcelDescription = null;
        this.size = ParcelSize.SMALL;
        this.receiver = null;
        this.priority = false;
        this.entryLocker = UUID.randomUUID();
        this.destinationLocker = null;
    }

    public String getParcelName() {
        return this.parcelName;
    }

    public void setParcelName(String parcelName) {
        this.parcelName = parcelName;
    }

    public String getParcelDescription() {
        return this.parcelDescription;
    }

    public void setParcelDescription(String parcelDescription) {
        this.parcelDescription = parcelDescription;
    }

    public ParcelSize getSize() {
        return this.size;
    }

    public void setSize(ParcelSize size) {
        this.size = size;
    }

    public UUID getReceiver() {
        return this.receiver;
    }

    public void setReceiver(UUID receiver) {
        this.receiver = receiver;
    }

    public boolean isPriority() {
        return this.priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public UUID getDestinationLocker() {
        return this.destinationLocker;
    }

    public void setDestinationLocker(UUID destinationLocker) {
        this.destinationLocker = destinationLocker;
    }

    public UUID getEntryLocker() {
        return this.entryLocker;
    }

    public void setEntryLocker(UUID entryLocker) {
        this.entryLocker = entryLocker;
    }
}