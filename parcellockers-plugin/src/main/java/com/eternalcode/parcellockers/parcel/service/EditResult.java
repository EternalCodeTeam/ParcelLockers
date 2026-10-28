package com.eternalcode.parcellockers.parcel.service;

public final class EditResult {

    public enum Status { OK, SIZE_TOO_SMALL, DESTINATION_FULL, PARCEL_COLLECTED }

    private final Status status;

    private EditResult(Status status) {
        this.status = status;
    }

    public static EditResult ok() {
        return new EditResult(Status.OK);
    }

    public static EditResult of(Status status) {
        return new EditResult(status);
    }

    public Status status() {
        return this.status;
    }

    public boolean isOk() {
        return this.status == Status.OK;
    }
}
