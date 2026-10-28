package com.eternalcode.parcellockers.shared;

public record Page(int page, int size) {

    public boolean hasPrevious() {
        return this.page > 0;
    }

    public int getOffset() {
        return this.page * this.size;
    }

    public int getLimit() {
        return this.size;
    }

    public Page next() {
        return new Page(this.page + 1, this.size);
    }

    public Page previous() {
        if (this.page == 0) {
            throw new IllegalStateException("Cannot go back from first page");
        }

        return new Page(this.page - 1, this.size);
    }

}
