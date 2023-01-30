package xyz.jakubk15.parcellockers.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString(includeFieldNames = true, callSuper = true)
@AllArgsConstructor

public class ParcelLocker {

	private Location location;
	private Map<UUID, List<Parcel>> parcelMap;
	private String name;
	private UUID uniqueId;

}
