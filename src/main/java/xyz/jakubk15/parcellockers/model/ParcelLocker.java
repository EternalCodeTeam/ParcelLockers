package xyz.jakubk15.parcellockers.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString(includeFieldNames = true, callSuper = true)

/*
 * A class representing parcel locker, objects spreaded in the world used to send parcels.
 */

public class ParcelLocker {

	public ParcelLocker(@NotNull Location loc, @Nullable Map<UUID, List<Parcel>> parcelMap, @NotNull String name, @NotNull int id) {
		this.loc = loc;
		this.parcelMap = parcelMap;
		this.name = name;
		this.id = id;
	}

	//* Parcel locker location.
	private Location loc;

	/*
	 * A parcel map
	 * @key Parcel UUID
	 * @value List of parcel on given UUID
	 */

	private Map<UUID, List<Parcel>> parcelMap;

	//* Parcel locker name
	private String name;

	//* Parcel locker ID
	private int id;

}
