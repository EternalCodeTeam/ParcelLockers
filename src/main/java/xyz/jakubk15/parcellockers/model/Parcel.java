package xyz.jakubk15.parcellockers.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class Parcel {

	public String parcelName;
	public Set<String> playerNames;
	public List<ItemStack> items;
	public ParcelSize size;

	public Parcel(final String parcelName, final Set<String> playerNames, final List<ItemStack> items, final ParcelSize size) {
		this.parcelName = parcelName;
		this.playerNames = playerNames;
		this.items = items;
		this.size = size;
	}

	public String getParcelName() {
		return parcelName;
	}

	public void setParcelName(final String parcelName) {
		this.parcelName = parcelName;
	}

	public Set<String> getPlayerNames() {
		return playerNames;
	}

	public void setPlayerNames(final Set<String> playerNames) {
		this.playerNames = playerNames;
	}

	public List<ItemStack> getItems() {
		return items;
	}

	public void setItems(final List<ItemStack> items) {
		this.items = items;
	}

	public ParcelSize getSize() {
		return size;
	}

	public void setSize(final ParcelSize size) {
		this.size = size;
	}
}
