package com.eternalcode.parcellockers.discord.controller;

import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.discord.DiscordLinkService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.event.ParcelDeliverEvent;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import reactor.core.scheduler.Schedulers;

public class ParcelDeliverNotificationController implements Listener {

    private final GatewayDiscordClient client;
    private final DiscordLinkService discordLinkService;
    private final UserManager userManager;
    private final MessageConfig messageConfig;
    private final Logger logger;

    public ParcelDeliverNotificationController(
        GatewayDiscordClient client,
        DiscordLinkService discordLinkService,
        UserManager userManager,
        MessageConfig messageConfig,
        Logger logger
    ) {
        this.client = client;
        this.discordLinkService = discordLinkService;
        this.userManager = userManager;
        this.messageConfig = messageConfig;
        this.logger = logger;
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
                String discordId = link.discordId();
                this.sendDeliveryNotification(parcel, discordId);
            }));
    }

    private void sendDeliveryNotification(Parcel parcel, String discordId) {
        CompletableFuture<String> senderNameFuture = this.userManager.get(parcel.sender())
            .thenApply(optionalUser -> optionalUser.map(User::name).orElse("Unknown"));

        CompletableFuture<String> receiverNameFuture = this.userManager.get(parcel.receiver())
            .thenApply(optionalUser -> optionalUser.map(User::name).orElse("Unknown"));

        senderNameFuture.thenCombine(receiverNameFuture, (senderName, receiverName) -> {
            String message = this.messageConfig.discord.parcelDeliveryNotification
                .replace("{PARCEL_NAME}", parcel.name())
                .replace("{SENDER}", senderName)
                .replace("{RECEIVER}", receiverName)
                .replace("{DESCRIPTION}", parcel.description() != null ? parcel.description() : "")
                .replace("{SIZE}", parcel.size().name())
                .replace("{PRIORITY}", parcel.priority() ? "Yes" : "No");

            this.client.getUserById(Snowflake.of(Long.parseLong(discordId)))
                .flatMap(user -> user.getPrivateChannel())
                .flatMap(channel -> channel.createMessage(message))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> this.logger.warning(
                    "Failed to send parcel delivery notification to Discord user " + discordId + ": " + error.getMessage()
                ))
                .subscribe();

            return null;
        });
    }
}
