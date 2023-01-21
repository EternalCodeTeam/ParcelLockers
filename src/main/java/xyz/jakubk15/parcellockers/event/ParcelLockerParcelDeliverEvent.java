package xyz.jakubk15.parcellockers.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

import java.util.List;

/**
 * <p>
 * Called when a parcel is delivered to a parcel locker
 * {@link ParcelLocker} is the parcel locker that the parcel was delivered to
 * {@link Parcel} is the parcel that was delivered
 * {@link Player} is the player that sent the parcel
 * {@link List<Player>} is the list of players that the parcel was sent to
 * {@link Location} is the location of the parcel locker
 * <p>
 */

public class ParcelLockerParcelDeliverEvent extends Event implements Listener {

	public ParcelLockerParcelDeliverEvent(Parcel parcel, long timestamp) {
		this.parcel = parcel;
		this.timestamp = timestamp;
	}

	private static final HandlerList handlerList = new HandlerList();
	private final Parcel parcel;
	private final long timestamp;

	public Parcel getParcel() {
		return parcel;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

}
