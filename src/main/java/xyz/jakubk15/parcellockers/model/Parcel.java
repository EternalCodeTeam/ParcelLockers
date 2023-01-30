package xyz.jakubk15.parcellockers.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
@ToString

/*
 * Model class for a Parcel, delivered by a parcel locker.
 */

public class Parcel {

	private String parcelName;
	private Set<String> playerNames;
	private List<ItemStack> items;
	private ParcelSize size;
	private boolean isPriority;
	private UUID uniqueId;
	private ParcelLocker entryParcelLocker;
	private ParcelLocker destinationParcelLocker;
	private Player sender;

	public static Parcel fromUUID(UUID uniqueId) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
			.filter(parcel -> parcel.getUniqueId().equals(uniqueId))
			.findFirst()
			.orElse(null);
	}

	@ApiStatus.Internal
	public static Parcel fromUUIDCancelled(UUID uniqueId) {
		return ParcelLockersPlugin.getInstance().getCancelledParcels()
			.stream()
			.filter(parcel -> parcel.getUniqueId().equals(uniqueId))
			.findFirst()
			.orElse(null);
	}

	public static List<Parcel> fromPlayerName(String playerName) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
			.filter(parcel -> parcel.getPlayerNames().contains(playerName))
			.collect(Collectors.toList());
	}

	public static List<Parcel> fromParcelLocker(ParcelLocker parcelLocker) {
		return new ArrayList<>(ParcelLockersPlugin.getInstance().getParcelDatabase()
			.get(parcelLocker));
	}

	public static List<Parcel> fromSender(Player player) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
			.filter(parcel -> parcel.getSender().getName().equals(player.getName()))
			.collect(Collectors.toList());
	}


}
