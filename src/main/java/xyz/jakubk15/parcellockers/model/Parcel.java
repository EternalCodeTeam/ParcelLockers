package xyz.jakubk15.parcellockers.model;

import lombok.*;
import org.bukkit.inventory.ItemStack;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor

/*
 * Model class for a Parcel, delivered by a parcel locker.
 */

public class Parcel {

	//* A parcel name
	private String parcelName;
	//* A set of player names bound to this parcel
	private Set<String> playerNames;
	//* A list of items in given parcel.
	private List<ItemStack> items;
	//* The size of the parcel.
	private ParcelSize size;
	//* The parcel priority boolean.
	private boolean isPriority;
	//* A parcel unique ID.
	private UUID uniqueId;

	public static Parcel fromUUID(final UUID uniqueId) {
		return ParcelLockersPlugin.getInstance().getParcelDatabase()
			.values()
			.stream()
			.flatMap(List::stream)
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


}
