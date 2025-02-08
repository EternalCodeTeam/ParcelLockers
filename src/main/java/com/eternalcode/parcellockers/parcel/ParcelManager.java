package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import org.bukkit.command.CommandSender;

public class ParcelManager {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository parcelRepository;

    public ParcelManager(PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepository parcelRepository) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
    }

    public void createParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.save(parcel).thenAccept(v ->
            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyCreated)
        ).whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                if (throwable != null) {
                    this.announcer.sendMessage(sender, this.config.messages.failedToCreateParcel);
                }
            }
        ));
    }

    public void deleteParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.remove(parcel).thenAccept(v ->
            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyDeleted)
        ).whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                if (throwable != null) {
                    this.announcer.sendMessage(sender, this.config.messages.failedToDeleteParcel);
                }
            }
        ));
    }
}
