package xyz.jakubk15.parcellockers;


import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class PlayerCache {

	public static volatile Map<UUID, PlayerCache> cacheMap = new HashMap<>();
	public int packagesReceived;
	public int awaitingPackages;
	public int sentPackages;
	public int returnedPackages;


	public static PlayerCache getCache(final Player player) {
		final PlayerCache cache = cacheMap.get(player.getUniqueId());

		if (cache != null) {
			cacheMap.put(player.getUniqueId(), cache);
		}
		return cacheMap.get(player);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final PlayerCache that = (PlayerCache) o;
		return packagesReceived == that.packagesReceived && awaitingPackages == that.awaitingPackages && sentPackages == that.sentPackages && returnedPackages == that.returnedPackages;
	}

	@Override
	public int hashCode() {
		return Objects.hash(packagesReceived, awaitingPackages, sentPackages, returnedPackages);
	}

	@Override
	public String toString() {
		return "PlayerCache{" +
				"packagesReceived=" + packagesReceived +
				", awaitingPackages=" + awaitingPackages +
				", sentPackages=" + sentPackages +
				", returnedPackages=" + returnedPackages +
				'}';
	}
}
