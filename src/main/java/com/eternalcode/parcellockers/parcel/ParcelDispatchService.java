package com.eternalcode.parcellockers.parcel;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelDispatchService {

    private static final Logger LOGGER = Logger.getLogger(ParcelDispatchService.class.getName());

    private final LockerManager lockerManager;
    private final ParcelService parcelService;
    private final DeliveryManager deliveryManager;
    private final ItemStorageManager itemStorageManager;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final NoticeService noticeService;

    public ParcelDispatchService(
        LockerManager lockerManager,
        ParcelService parcelService,
        DeliveryManager deliveryManager,
        ItemStorageManager itemStorageManager,
        Scheduler scheduler,
        PluginConfig config,
        NoticeService noticeService
    ) {
        this.lockerManager = lockerManager;
        this.parcelService = parcelService;
        this.deliveryManager = deliveryManager;
        this.itemStorageManager = itemStorageManager;
        this.scheduler = scheduler;
        this.config = config;
        this.noticeService = noticeService;
    }

    public void dispatch(Player sender, Parcel parcel, List<ItemStack> items) {
        this.lockerManager.isLockerFull(parcel.destinationLocker())
            .thenCompose(isFull -> {
                if (isFull) {
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.lockerFull);
                    return CompletableFuture.completedFuture(null);
                }

                Duration delay = parcel.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;

                return this.parcelService.send(sender, parcel, items)
                    .thenCompose(success -> {
                        if (!Boolean.TRUE.equals(success)) {
                            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                            return CompletableFuture.completedFuture(null);
                        }

                        return this.itemStorageManager.delete(sender.getUniqueId())
                            .thenAccept(deleted -> {
                                if (!Boolean.TRUE.equals(deleted)) {
                                    this.parcelService.delete(parcel.uuid()); // Implement this method
                                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                                    return;
                                }

                                this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));

                                ParcelSendTask task = new ParcelSendTask(
                                    parcel,
                                    this.parcelService,
                                    this.deliveryManager
                                );

                                this.scheduler.runLaterAsync(task, delay);
                            });
                    });
            })
            .exceptionally(throwable -> {
                LOGGER.severe("Failed to dispatch parcel for player " + sender.getName() + ": " + throwable.getMessage());
                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                return null;
            });
    }
}
