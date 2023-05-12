package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import org.bukkit.plugin.Plugin;

public class ParcelManager {

    private final Plugin plugin;
    private final ParcelDatabaseService databaseService;
    private final ParcelLockerDatabaseService parcelLockerDatabaseService;

    public ParcelManager(Plugin plugin, ParcelDatabaseService databaseService, ParcelLockerDatabaseService parcelLockerDatabaseService) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
    }
}
