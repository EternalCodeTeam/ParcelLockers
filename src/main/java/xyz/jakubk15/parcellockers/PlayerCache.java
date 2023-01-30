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
@ToString

public class PlayerCache {

	protected static volatile Map<UUID, PlayerCache> cacheMap = new HashMap<>();
	public int packagesReceived;
	public int awaitingPackages;
	public int sentPackages;
	public int returnedPackages;

	public static PlayerCache getCache(Player player) {
		PlayerCache cache = cacheMap.get(player.getUniqueId());

		if (cache != null) {
			cacheMap.put(player.getUniqueId(), cache);
		}
		return cacheMap.get(player.getUniqueId());
	}
}
