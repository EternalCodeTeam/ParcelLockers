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
	
	// A parcel name
	private String parcelName;
	// A set of player names bound to this parcel
	private Set<String> playerNames;
	// A list of items in given parcel.
	private List<ItemStack> items;
	// The size of the parcel.
	private ParcelSize size;
	// The parcel priority boolean.
	private boolean isPriority;
	// A parcel unique ID.
	private UUID uniqueId;
	// A parcel locker that this parcel was sent in.
	private ParcelLocker entryParcelLocker;
	// A parcel locker that this parcel is stored in.
	private ParcelLocker destinationParcelLocker;
	// A parcel sender
	private Player sender;

	public static Parcel fromUUID(final UUID uniqueId) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
			.filter(parcel -> parcel.getUniqueId().equals(uniqueId))
			.findFirst()
			.orElse(null);
	}

	@ApiStatus.Internal
	public static Parcel fromUUIDCancelled(final UUID uniqueId) {
		return ParcelLockersPlugin.getInstance().getCancelledParcels()
			.stream()
			.filter(parcel -> parcel.getUniqueId().equals(uniqueId))
			.findFirst()
			.orElse(null);
	}

	public static List<Parcel> fromPlayerName(final String playerName) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
			.filter(parcel -> parcel.getPlayerNames().contains(playerName))
			.collect(Collectors.toList());
	}

	public static List<Parcel> fromParcelLocker(final ParcelLocker parcelLocker) {
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
