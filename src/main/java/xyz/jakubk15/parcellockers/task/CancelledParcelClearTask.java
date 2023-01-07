package xyz.jakubk15.parcellockers.task;

import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;

public class CancelledParcelClearTask extends BukkitRunnable {

	@Override
	public void run() {
		Common.runTimerAsync(0, 20 * 60 * 360, () -> {
			Common.log("Clearing cancelled parcels...");
			ParcelLockersPlugin.getInstance().getCancelledParcels().clear();
			Common.broadcastWithPerm("parcellockers.admin", "&aCancelled parcels have been cleared!", true);
		});
	}
}
