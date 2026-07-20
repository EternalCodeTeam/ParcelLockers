package com.eternalcode.parcellockers.shared.exception;

/**
 * Base exception for the ParcelLockers plugin.
 * All plugin-specific exceptions should extend this class.
 */
public class ParcelLockersException extends RuntimeException {

    public ParcelLockersException(String message) {
        super(message);
    }

    public ParcelLockersException(String message, Throwable cause) {
        super(message, cause);
    }
}

