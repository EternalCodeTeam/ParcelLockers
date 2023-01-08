package xyz.jakubk15.parcellockers.listener;

import org.bukkit.event.Listener;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.event.ParcelLockerBreakEvent;

public class ParcelLockerBreakListener implements Listener {

	private final ParcelLockersPlugin plugin;

	public ParcelLockerBreakListener(ParcelLockersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onParcelLockerBreak(ParcelLockerBreakEvent event) {
		this.plugin.getParcelDatabase()
			.keySet()
			.stream()
			.filter(parcelLocker -> parcelLocker.getLocation().equals(event.getLocation()))
			.forEach(parcelLocker -> this.plugin.getParcelDatabase()
				.remove(parcelLocker));
	}

}
