package xyz.jakubk15.parcellockers.model;

import lombok.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class Parcel {

	private String parcelName;
	private Set<String> playerNames;
	private List<ItemStack> items;
	private ParcelSize size;
	private boolean isPriority;
	private UUID uniqueId;

	/* TODO
	public static Parcel fromUUID(UUID uniqueId) {

	}
	*/
}
