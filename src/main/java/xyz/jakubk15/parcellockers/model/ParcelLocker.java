package xyz.jakubk15.parcellockers.model;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class ParcelLocker {

	public Location loc;
	public Map<UUID, List<Parcel>> parcelMap = new TreeMap<>();
	public String name;
	public int id;

	public ParcelLocker(@NotNull final Location loc, @NotNull final String name, @NotNull final int id, @NotNull final Map parcelMap) {
		this.loc = loc;
		this.name = name;
		this.id = id;
		this.parcelMap = parcelMap;
	}

	public Location getLoc() {
		return loc;
	}

	public void setLoc(final Location loc) {
		this.loc = loc;
	}

	public Map<UUID, List<Parcel>> getParcelMap() {
		return parcelMap;
	}

	public void setParcelMap(final Map<UUID, List<Parcel>> parcelMap) {
		this.parcelMap = parcelMap;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}
}
