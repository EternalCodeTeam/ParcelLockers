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

	public String parcelName;
	public Set<String> playerNames;
	public List<ItemStack> items;
	public ParcelSize size;
	public boolean isPriority;
	public UUID uniqueId;

	/* TODO
	public static Parcel fromUUID(UUID uniqueId) {

	}
	*/
}
