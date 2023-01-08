package xyz.jakubk15.parcellockers.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import xyz.jakubk15.parcellockers.event.ParcelLockerDropEvent;

public class ParcelLockerDropListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onParcelLockerDrop(final ParcelLockerDropEvent event) {
		Common.tellTimed(3, event.getPlayer(), "&cParcel locker dropping has been disabled by default in the settings.yml file.");
		event.setCancelled(true);
	}

}
