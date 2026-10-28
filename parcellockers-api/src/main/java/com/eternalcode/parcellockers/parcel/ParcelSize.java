package com.eternalcode.parcellockers.parcel;

public enum ParcelSize {
    SMALL(9),
    MEDIUM(18),
    LARGE(27);

    private final int capacity;

    ParcelSize(int capacity) {
        this.capacity = capacity;
    }

    /**
     * @return the maximum number of item stacks a parcel of this size can hold
     */
    public int capacity() {
        return this.capacity;
    }
}
