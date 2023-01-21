package xyz.jakubk15.parcellockers.exception;


/*
 * Exception thrown when the parcel is not found.
 */

public class ParcelNotFoundException extends Exception {

	public ParcelNotFoundException() {
		super();
	}

	public ParcelNotFoundException(String message) {
		super(message);
	}

	public ParcelNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParcelNotFoundException(Throwable cause) {
		super(cause);
	}
}
