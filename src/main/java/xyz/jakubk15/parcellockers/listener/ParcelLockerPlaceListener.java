package xyz.jakubk15.parcellockers.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.event.ParcelLockerPlaceEvent;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

public class ParcelLockerPlaceListener implements Listener {

	private final ParcelLockersPlugin plugin;

	public ParcelLockerPlaceListener(ParcelLockersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onParcelLockerPlace(ParcelLockerPlaceEvent event) {
		this.plugin.getParcelDatabase().put(new ParcelLocker(event.getLocation(), null, null, this.plugin.getParcelDatabase().size() + 1), null);
	}

}
