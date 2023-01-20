package xyz.jakubk15.parcellockers;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString(includeFieldNames = true, callSuper = true)

/**
 * A class representing the plugin's player cache.
 */

public class PlayerCache {

	/**
	 * A cache map.
	 */

	public static volatile Map<UUID, PlayerCache> cacheMap = new HashMap<>();

	/**
	 * A number of packages received by player.
	 */

	public int packagesReceived;

	/**
	 * A number of packages delivered to player, waiting for pickup.
	 */

	public int awaitingPackages;

	/**
	 * A number of packages given player has sent.
	 */

	public int sentPackages;

	/**
	 * A number of packages given player has returned.
	 */
	public int returnedPackages;

	/**
	 * The cache getter.
	 */

	public static PlayerCache getCache(final Player player) {
		final PlayerCache cache = cacheMap.get(player.getUniqueId());

		if (cache != null) {
			cacheMap.put(player.getUniqueId(), cache);
		}
		return cacheMap.get(player.getUniqueId());
	}
}
