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

	public Location loc;
	public Map<UUID, List<Parcel>> parcelMap;
	public String name;
	public int id;

}
