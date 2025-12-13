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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelDispatchService {

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
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.lockerFull)
                        .player(sender.getUniqueId())
                        .send();
                    return CompletableFuture.completedFuture(null);
                }

                Duration delay = parcel.priority()
                    ? this.config.settings.priorityParcelSendDuration
                    : this.config.settings.parcelSendDuration;

                return this.parcelService.send(sender, parcel, items)
                    .thenAccept(success -> {
                        if (!Boolean.TRUE.equals(success)) {
                            return;
                        }

                        this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));

                        ParcelSendTask task = new ParcelSendTask(
                            parcel,
                            this.parcelService,
                            this.deliveryManager
                        );

                        this.itemStorageManager.delete(sender.getUniqueId()).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            return false;
                        });

                        this.scheduler.runLaterAsync(task, delay);
                    });
            })
            .exceptionally(throwable -> {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.cannotSend)
                    .player(sender.getUniqueId())
                    .send();
                return null;
            });
    }
}
