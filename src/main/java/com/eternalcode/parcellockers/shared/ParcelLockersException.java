package com.eternalcode.parcellockers.shared;

public class ParcelLockersException extends RuntimeException {

    public ParcelLockersException() {
    }

    public ParcelLockersException(String message) {
        super(message);
    }

    public ParcelLockersException(String message, Throwable cause) {
        super(message, cause);
    }
}
