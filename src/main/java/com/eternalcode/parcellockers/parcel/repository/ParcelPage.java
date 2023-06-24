package com.eternalcode.parcellockers.parcel.repository;

public record ParcelPage(int page, int size) {

    public boolean hasPrevious() {
        return this.page > 0;
    }

    public int getOffset() {
        return this.page * this.size;
    }

    public int getLimit() {
        return this.size;
    }

    public ParcelPage next() {
        return new ParcelPage(this.page + 1, this.size);
    }

    public ParcelPage previous() {
        if (this.page == 0) {
            throw new IllegalStateException("Cannot go back from first page");
        }

        return new ParcelPage(this.page - 1, this.size);
    }

}
