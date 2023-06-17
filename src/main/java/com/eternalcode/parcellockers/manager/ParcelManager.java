package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelLocker;
import com.eternalcode.parcellockers.parcel.ParcelLockerRepository;
import com.eternalcode.parcellockers.parcel.ParcelMeta;
import com.eternalcode.parcellockers.parcel.ParcelRepository;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.shared.Position;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParcelManager {

    private final ParcelRepository parcelRepository;
    private final ParcelLockerRepository parcelLockerRepository;

    public ParcelManager(ParcelRepository repository, ParcelLockerRepository parcelLockerRepository) {
        this.parcelRepository = repository;
        this.parcelLockerRepository = parcelLockerRepository;
    }

    public void saveTestParcel() {
        ParcelLocker entry = new ParcelLocker(UUID.randomUUID(), "Entry locker", new Position(0, 100, 0, 0, 0, "world"));
        ParcelLocker destination = new ParcelLocker(UUID.randomUUID(), "Destination locker", new Position(1, 100, 0, 0, 0, "world"));
        this.parcelLockerRepository.save(entry);
        this.parcelLockerRepository.save(destination);

        this.parcelRepository.save(Parcel.builder()
                .sender(UUID.fromString("fe088faa-9291-4e27-936e-2e1ebdd3bbb4"))
                .meta(new ParcelMeta("Test Parcel", "Test parcel description", true, UUID.fromString("fe088faa-9291-4e27-936e-2e1ebdd3bbb4"), ParcelSize.LARGE, entry, destination))
                .uuid(UUID.randomUUID())
                .build());
    }

    public void listAll(Player player) {
        this.parcelRepository.findAll().thenAccept(parcels -> {
            player.sendMessage("Parcels: " + parcels.size());
            parcels.forEach(parcel -> player.sendMessage(parcel.uuid().toString()));
        });
    }

}