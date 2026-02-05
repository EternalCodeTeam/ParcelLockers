package com.eternalcode.parcellockers.discord.controller;

import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.discord.notification.DiscordNotificationService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.event.ParcelDeliverEvent;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DiscordDeliverNotificationController implements Listener {

    private final DiscordNotificationService notificationService;
    private final DiscordLinkService discordLinkService;
    private final UserManager userManager;
    private final MessageConfig messageConfig;

    public DiscordDeliverNotificationController(
        DiscordNotificationService notificationService,
        DiscordLinkService discordLinkService,
        UserManager userManager,
        MessageConfig messageConfig
    ) {
        this.notificationService = notificationService;
        this.discordLinkService = discordLinkService;
        this.userManager = userManager;
        this.messageConfig = messageConfig;
    }

    @EventHandler
    void onParcelDeliver(ParcelDeliverEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Parcel parcel = event.getParcel();
        UUID receiverUuid = parcel.receiver();

        this.discordLinkService.findLinkByPlayer(receiverUuid)
            .thenAccept(optionalLink -> optionalLink.ifPresent(link -> {
                this.sendDeliveryNotification(parcel, link.discordId());
            }));
    }

    private void sendDeliveryNotification(Parcel parcel, long discordId) {
        CompletableFuture<String> senderNameFuture = this.userManager.get(parcel.sender())
            .thenApply(optionalUser -> optionalUser.map(User::name).orElse("Unknown"));

        CompletableFuture<String> receiverNameFuture = this.userManager.get(parcel.receiver())
            .thenApply(optionalUser -> optionalUser.map(User::name).orElse("Unknown"));

        senderNameFuture.thenAcceptBoth(receiverNameFuture, (senderName, receiverName) -> {
            String message = this.messageConfig.discord.parcelDeliveryNotification;
            Formatter formatter = new Formatter()
                .register("{PARCEL_NAME}", parcel.name())
                .register("{SENDER}", senderName)
                .register("{RECEIVER}", receiverName)
                .register("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "No description")
                .register("{SIZE}", parcel.size().name())
                .register("{PRIORITY}", parcel.priority()
                    ? this.messageConfig.discord.highPriorityPlaceholder
                    : this.messageConfig.discord.normalPriorityPlaceholder
                );

            this.notificationService.sendPrivateMessage(discordId, formatter.format(message));
        });
    }
}
