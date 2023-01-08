package xyz.jakubk15.parcellockers.model;

import lombok.*;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString(includeFieldNames = true, callSuper = true)
@AllArgsConstructor

/*
 * A class representing parcel locker, objects spreaded in the world used to send parcels.
 */

public class ParcelLocker {

	//* Parcel locker location.
	private Location location;

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
