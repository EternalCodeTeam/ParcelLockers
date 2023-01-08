package xyz.jakubk15.parcellockers.manager;

import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.exception.ParcelNotFoundException;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

public class ParcelManager {

	// The main plugin class instance.
	private final ParcelLockersPlugin plugin;

	public ParcelManager(final ParcelLockersPlugin plugin) {
		this.plugin = plugin;
	}

	// Method for sending parcels.
	public void sendParcel(final Parcel parcel, final ParcelLocker locker) {
		this.plugin.getParcelDatabase().get(locker).add(parcel);
	}

	// Method for cancelling sent parcels.
	public void cancelParcel(final Parcel parcel) throws ParcelNotFoundException {
		try {
			if (Parcel.fromUUID(parcel.getUniqueId()) != null) {
				this.plugin.getCancelledParcels().add(parcel);
				this.plugin.getParcelDatabase()
					.values()
					.forEach(parcels -> this.plugin.getParcelDatabase()
						.values()
						.remove(parcels));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ParcelNotFoundException("This parcel is not found.");
		}
	}

	public void undoCancel(final Parcel parcel) throws ParcelNotFoundException {
		try {
			if (Parcel.fromUUIDCancelled(parcel.getUniqueId()) != null) {
				this.plugin.getCancelledParcels().remove(parcel);
				this.plugin.getParcelDatabase()
					.values()
					.forEach(parcels -> this.plugin.getParcelDatabase()
						.values()
						.add(parcels));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ParcelNotFoundException("This parcel is not found.");
		}
	}


	// Method for deleting sent parcels.
	public void deleteParcel(final Parcel parcel) throws ParcelNotFoundException {
		try {
			this.plugin.getParcelDatabase()
				.values()
				.forEach(parcels -> parcels.remove(parcel));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ParcelNotFoundException("This parcel is not found.");
		}
	}

}
