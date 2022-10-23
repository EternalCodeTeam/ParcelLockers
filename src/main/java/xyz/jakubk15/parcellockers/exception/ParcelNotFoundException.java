package xyz.jakubk15.parcellockers.exception;


/*
 * Exception throwed when the parcel is not found.
 */

public class ParcelNotFoundException extends Exception {

	public ParcelNotFoundException() {
		super();
	}

	public ParcelNotFoundException(final String message) {
		super(message);
	}

	public ParcelNotFoundException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ParcelNotFoundException(final Throwable cause) {
		super(cause);
	}
}
