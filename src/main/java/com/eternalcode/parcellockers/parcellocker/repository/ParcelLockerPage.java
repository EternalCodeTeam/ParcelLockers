package com.eternalcode.parcellockers.parcellocker.repository;

public record ParcelLockerPage(int page, int size) {

    public boolean hasPrevious() {
        return this.page > 0;
    }

    public int getOffset() {
        return this.page * this.size;
    }

    public int getLimit() {
        return this.size;
    }

    public ParcelLockerPage next() {
        return new ParcelLockerPage(this.page + 1, this.size);
    }

    public ParcelLockerPage previous() {
        if (this.page == 0) {
            throw new IllegalStateException("Cannot go back from first page");
        }

        return new ParcelLockerPage(this.page - 1, this.size);
    }

}
