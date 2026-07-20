package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    // Serializes dispatches per destination locker so that two concurrent sends cannot both pass
    // the fullness check before either parcel is persisted (a TOCTOU that could exceed the cap).
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> lockerChains = new ConcurrentHashMap<>();

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
        UUID lockerId = parcel.destinationLocker();

        CompletableFuture<Void> chained = this.lockerChains.compute(lockerId, (id, previous) -> {
            CompletableFuture<Void> predecessor = previous == null
                ? CompletableFuture.completedFuture(null)
                : previous.exceptionally(throwable -> null);
            return predecessor.thenCompose(ignored -> this.dispatchInternal(sender, parcel, items));
        });

        // Drop the chain entry once it drains so the map does not grow unbounded.
        chained.whenComplete((result, throwable) -> this.lockerChains.remove(lockerId, chained));
    }

    private CompletableFuture<Void> dispatchInternal(Player sender, Parcel parcel, List<ItemStack> items) {
        return this.lockerManager.isLockerFull(parcel.destinationLocker())
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
                            // A failed delete must trigger the rollback, not skip straight to the outer
                            // exceptionally handler (which would leave the parcel sent and the fee charged).
                            .exceptionally(throwable -> false)
                            .thenCompose(deleted -> {
                                if (!Boolean.TRUE.equals(deleted)) {
                                    // The parcel and its content were already persisted and the fee charged,
                                    // but the sender's staged storage could not be cleared. Fully roll back
                                    // (parcel + content + fee) instead of leaving orphaned content behind.
                                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                                    return this.parcelService.rollbackSend(sender, parcel);
                                }

                                this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));

                                ParcelSendTask task = new ParcelSendTask(
                                    parcel,
                                    this.parcelService,
                                    this.deliveryManager,
                                    this.scheduler
                                );

                                this.scheduler.runLaterAsync(task, delay);
                                // Only confirm success here, once every step has succeeded, to avoid a
                                // "sent" notice immediately followed by "cannot send" on a rollback.
                                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
                                return CompletableFuture.completedFuture(null);
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
