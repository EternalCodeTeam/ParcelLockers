package xyz.jakubk15.parcellockers.task;

import org.bukkit.scheduler.BukkitRunnable;
import panda.std.Pair;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;

import java.util.HashMap;
import java.util.Map;

public class ParcelAutoReturnTask extends BukkitRunnable {

	private Map<ParcelLocker, Pair<Parcel, Long>> map = new HashMap<>();

	@Override
	public void run() {
		// TODO
	}
}
