package xyz.jakubk15.parcellockers;


import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class PlayerCache {

	public static volatile Map<UUID, PlayerCache> cacheMap = new HashMap<>();
	public int packagesReceivedAmountTotal;
	public int awaitingPackages;
	public int sentPackagesTotal;
	public int returnedPackagesTotal;


	public static PlayerCache getCache(final Player player) {
		final PlayerCache cache = cacheMap.get(player.getUniqueId());

		if (cache != null) {
			cacheMap.put(player.getUniqueId(), cache);
		}
		return cacheMap.get(player);
	}


}
