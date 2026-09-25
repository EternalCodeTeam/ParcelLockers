package com.eternalcode.parcellockers.shared.exception;

/**
 * Exception thrown when a database operation fails.
 */
public class DatabaseException extends ParcelLockersException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}
