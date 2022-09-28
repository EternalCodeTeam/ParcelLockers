package xyz.jakubk15.parcellockers.model;

import lombok.*;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class ParcelLocker {

	private Location loc;
	private Map<UUID, List<Parcel>> parcelMap;
	private String name;
	private int id;

}
