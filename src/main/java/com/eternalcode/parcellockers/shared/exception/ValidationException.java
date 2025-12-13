package com.eternalcode.parcellockers.shared.exception;

/**
 * Exception thrown when a validation fails.
 */
public class ValidationException extends ParcelLockersException {

    public ValidationException(String message) {
        super(message);
    }
}
