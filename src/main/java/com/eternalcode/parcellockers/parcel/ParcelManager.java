package com.eternalcode.parcellockers.parcel;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import panda.std.Blank;
import panda.std.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

public class ParcelManager {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository parcelRepository;
    private final DeliveryRepository deliveryRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;

    public ParcelManager(PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepository parcelRepository, DeliveryRepository deliveryRepository, ParcelContentRepository parcelContentRepository, Scheduler scheduler) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
        this.deliveryRepository = deliveryRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Result<Blank, Throwable>> sendParcel(CommandSender sender, Parcel parcel, List<ItemStack> items) {
        Duration delay = parcel.priority()
            ? config.settings.priorityParcelSendDuration
            : config.settings.parcelSendDuration;

        parcelRepository.save(parcel);

        return parcelContentRepository.save(new ParcelContent(parcel.uuid(), items))
            .handle((content, throwable) -> {
                if (throwable != null) {
                    announcer.sendMessage(sender, config.messages.parcelFailedToSend);
                    return Result.error(throwable);
                }

                announcer.sendMessage(sender, config.messages.parcelSent);

                scheduler.runLaterAsync(
                    new ParcelSendTask(
                        parcel,
                        new Delivery(parcel.uuid(), Instant.now().plus(delay)),
                        parcelRepository,
                        deliveryRepository,
                        config
                    ),
                    delay
                );

                return Result.ok();
            });
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

    public void collectParcel(Player player, Parcel parcel) {
        this.parcelContentRepository.findByUUID(parcel.uuid()).thenAccept(optional -> {
            if (optional.isEmpty()) {
                player.playSound(player.getLocation(), this.config.settings.errorSound, this.config.settings.errorSoundVolume, this.config.settings.errorSoundPitch);
                this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                player.playSound(player.getLocation(), this.config.settings.errorSound, this.config.settings.errorSoundVolume, this.config.settings.errorSoundPitch);
                this.announcer.sendMessage(player, this.config.messages.notEnoughInventorySpace);
                return;
            }

            for (ItemStack item : items) {
                this.scheduler.run(() -> ItemUtil.giveItem(player, item));
            }

            this.parcelRepository.remove(parcel)
                .thenCompose(v -> this.parcelContentRepository.remove(optional.get().uniqueId()))
                .whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                        if (throwable != null) {
                            this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                            return;
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1);
                        this.announcer.sendMessage(player, this.config.messages.parcelSuccessfullyCollected);
                    }
                ));
        });
    }
}
