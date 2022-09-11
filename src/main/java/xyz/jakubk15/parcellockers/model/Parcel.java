package xyz.jakubk15.parcellockers.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class Parcel {

	public String parcelName;
	public Set<String> playerNames;
	public List<ItemStack> items;
	public ParcelSize size;
	public boolean isPriority;
}
