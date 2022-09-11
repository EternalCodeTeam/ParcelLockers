package xyz.jakubk15.parcellockers.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import xyz.jakubk15.parcellockers.event.ParcelLockerDropEvent;

public class ParcelLockerDropListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onParcelLockerDrop(final ParcelLockerDropEvent event) {
		event.setCancelled(true);
	}

}
