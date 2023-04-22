package com.eternalcode.parcellockers.exception;

public class ParcelLockersException extends RuntimeException {

    public ParcelLockersException() {
    }

    public ParcelLockersException(String message) {
        super(message);
    }

    public ParcelLockersException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParcelLockersException(Throwable cause) {
        super(cause);
    }

    public ParcelLockersException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
