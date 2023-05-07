package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelLocker;
import com.eternalcode.parcellockers.parcel.ParcelMeta;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.shared.Position;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParcelManager {

    private final ParcelDatabaseService databaseService;
    private final ParcelLockerDatabaseService parcelLockerDatabaseService;

    public ParcelManager(ParcelDatabaseService databaseService, ParcelLockerDatabaseService parcelLockerDatabaseService) {
        this.databaseService = databaseService;
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
    }

    public void saveTestParcel() {
        ParcelLocker entry = new ParcelLocker(UUID.randomUUID(), "Entry locker", new Position(0, 100, 0, 0, 0, "world"));
        ParcelLocker destination = new ParcelLocker(UUID.randomUUID(), "Destination locker", new Position(1, 100, 0, 0, 0, "world"));
        this.parcelLockerDatabaseService.save(entry);
        this.parcelLockerDatabaseService.save(destination);

        this.databaseService.save(Parcel.builder()
                .sender(UUID.fromString("fe088faa-9291-4e27-936e-2e1ebdd3bbb4"))
                .meta(new ParcelMeta("Test Parcel", "Test parcel description", true, UUID.fromString("fe088faa-9291-4e27-936e-2e1ebdd3bbb4"), ParcelSize.LARGE, entry, destination))
                .uuid(UUID.randomUUID())
                .build());
    }

    public void listAll(Player player) {
        this.databaseService.findAll().whenComplete((parcels, throwable) -> {
            parcels.forEach(parcel -> player.sendMessage("§7- " + parcel.meta()));
        });
    }
}
