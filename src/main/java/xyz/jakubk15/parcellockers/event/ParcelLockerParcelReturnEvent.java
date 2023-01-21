package xyz.jakubk15.parcellockers.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

public class ParcelLockerParcelReturnEvent extends Event implements Listener {

	public ParcelLockerParcelReturnEvent(ParcelLocker from, Parcel parcel, long time) {
		this.from = from;
		this.parcel = parcel;
		this.time = time;
	}

	private final static HandlerList handlerList = new HandlerList();
	private final ParcelLocker from;
	private final Parcel parcel;
	private final long time;

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	@NotNull
	public HandlerList getHandlerList() {
		return handlerList;
	}
}
