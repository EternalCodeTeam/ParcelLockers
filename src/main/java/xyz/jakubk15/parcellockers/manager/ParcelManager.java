package xyz.jakubk15.parcellockers.manager;

import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

import java.util.UUID;

public class ParcelManager {

	// The main plugin class instance.
	private final ParcelLockersPlugin plugin;

	public ParcelManager(final ParcelLockersPlugin plugin) {
		this.plugin = plugin;
	}

	// Method for sending parcels.
	public void sendParcel(final UUID uniqueId, final Parcel parcel, final ParcelLocker locker) {
		this.plugin.getParcels().add(parcel);
	}

	// Method for cancelling sent parcels.
	public void cancelParcel(final Parcel parcel) {
		if (Parcel.fromUUID(parcel.getUniqueId()) != null) {
			this.plugin.getCancelledParcels().add(parcel);
			this.plugin.getParcels().remove(parcel);
		}
	}

	// Method for deleting sent parcels.
	public void deleteParcel(final Parcel parcel) {
		this.plugin.getParcels().remove(parcel);
	}

}
