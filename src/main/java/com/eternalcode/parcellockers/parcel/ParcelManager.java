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
import java.util.List;

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

    public Result<Blank, Exception> sendParcel(CommandSender sender, Parcel parcel, List<ItemStack> items) {
        System.out.println("scheduled parcel: " + parcel);
        Duration delay = parcel.priority() ? this.config.settings.priorityParcelSendDuration : this.config.settings.parcelSendDuration;
        this.parcelRepository.save(parcel);
        this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items)).whenComplete((content, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(sender, this.config.messages.parcelFailedToSend);
                return;
            }
            this.announcer.sendMessage(sender, this.config.messages.parcelSent);
        });

        this.scheduler.runLaterAsync(new ParcelSendTask(parcel,
            new Delivery(parcel.uuid(), System.currentTimeMillis() + delay.toMillis()),
            this.parcelRepository,
            this.deliveryRepository,
            this.config),
            delay);

        return Result.ok();
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
                player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5F, 1);
                this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5F, 1);
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
