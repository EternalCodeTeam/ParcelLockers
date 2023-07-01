package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ParcelManager {

    private final Plugin plugin;
    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository databaseService;
    private final ParcelLockerRepository parcelLockerRepository;

    public ParcelManager(Plugin plugin, PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepository databaseService, ParcelLockerRepository parcelLockerRepository) {
        this.plugin = plugin;
        this.config = config;
        this.announcer = announcer;
        this.databaseService = databaseService;
        this.parcelLockerRepository = parcelLockerRepository;
    }

    public void createParcel(CommandSender sender, Parcel parcel) {
        this.databaseService.save(parcel).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(sender, this.config.messages.failedToCreateParcel);
                throwable.printStackTrace();
                return;
            }

            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyCreated);
        });
    }

    public void deleteParcel(CommandSender sender, Parcel parcel) {
        this.databaseService.remove(parcel).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(sender, this.config.messages.failedToDeleteParcel);
                throwable.printStackTrace();
                return;
            }

            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyDeleted);
        });
    }
}
