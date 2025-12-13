package com.eternalcode.parcellockers.shared.exception;

/**
 * Exception thrown when a parcel operation fails.
 */
public class ParcelOperationException extends ParcelLockersException {

    public ParcelOperationException(String message) {
        super(message);
    }

    public ParcelOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
