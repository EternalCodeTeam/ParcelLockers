package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import org.bukkit.command.CommandSender;

public class ParcelManager {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepositoryImpl parcelRepository;

    public ParcelManager(PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepositoryImpl parcelRepository) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
    }

    public void createParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.save(parcel).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(sender, this.config.messages.failedToCreateParcel);
                throwable.printStackTrace();
                return;
            }

            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyCreated);
        });
    }

    public void deleteParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.remove(parcel).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(sender, this.config.messages.failedToDeleteParcel);
                throwable.printStackTrace();
                return;
            }

            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyDeleted);
        });
    }
}
