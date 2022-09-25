package xyz.jakubk15.parcellockers.manager;

import lombok.experimental.UtilityClass;
import org.bukkit.OfflinePlayer;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

@UtilityClass
public class ParcelManager {

	private final ParcelLockersPlugin instance = ParcelLockersPlugin.getInstance();

	public void sendParcel(final OfflinePlayer player, final Parcel parcel, final ParcelLocker locker) {
		instance.parcelSet.add(parcel);
		
	}

	public void cancelParcel(final Parcel parcel) {

	}

	public void deleteParcel(final Parcel parcel) {
		instance.parcelSet.remove(parcel);
	}

}
