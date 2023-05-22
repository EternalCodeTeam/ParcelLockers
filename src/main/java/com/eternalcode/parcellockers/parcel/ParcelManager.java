package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import org.bukkit.plugin.Plugin;

public class ParcelManager {

    private final Plugin plugin;
    private final ParcelRepository databaseService;
    private final ParcelLockerRepository parcelLockerRepository;

    public ParcelManager(Plugin plugin, ParcelRepository databaseService, ParcelLockerRepository parcelLockerRepository) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.parcelLockerRepository = parcelLockerRepository;
    }
}
