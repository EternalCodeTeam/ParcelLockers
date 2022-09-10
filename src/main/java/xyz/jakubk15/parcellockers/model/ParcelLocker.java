package xyz.jakubk15.parcellockers.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class ParcelLocker {
	
	public Location loc;
	public Map<UUID, List<ItemStack>> itemsMap = new TreeMap<>();
	public String name;
	public int id;

	public ParcelLocker(@NotNull final Location loc, @NotNull final String name, @NotNull final int id, @NotNull final Map itemsMap) {
		this.loc = loc;
		this.name = name;
		this.id = id;
		this.itemsMap = itemsMap;
	}
}
