package com.eternalcode.parcellockers.gui;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiManager {

    private final PluginConfig config;
    private final Scheduler scheduler;
    private final ParcelService parcelService;
    private final LockerManager lockerManager;
    private final UserManager userManager;
    private final ItemStorageManager itemStorageManager;
    private final ParcelContentManager parcelContentManager;
    private final DeliveryManager deliveryManager;

    public GuiManager(
        PluginConfig config, Scheduler scheduler, ParcelService parcelService,
        LockerManager lockerManager,
        UserManager userManager,
        ItemStorageManager itemStorageManager,
        ParcelContentManager parcelContentManager,
        DeliveryManager deliveryManager
    ) {
        this.config = config;
        this.scheduler = scheduler;
        this.parcelService = parcelService;
        this.lockerManager = lockerManager;
        this.userManager = userManager;
        this.itemStorageManager = itemStorageManager;
        this.parcelContentManager = parcelContentManager;
        this.deliveryManager = deliveryManager;
    }

    public void sendParcel(Player sender, Parcel parcel, List<ItemStack> items) {
        Duration delay = parcel.priority()
            ? this.config.settings.priorityParcelSendDuration
            : this.config.settings.parcelSendDuration;
        this.parcelService.send(sender, parcel, items);
        this.deliveryManager.create(parcel.uuid(), Instant.now().plus(delay));
        this.parcelContentManager.create(parcel.uuid(), items);

        ParcelSendTask task = new ParcelSendTask(
            parcel,
            this.parcelService,
            this.deliveryManager
        );

        this.scheduler.runLaterAsync(task, delay);
    }

    public void collectParcel(Player player, Parcel parcel) {
        this.parcelService.collect(player, parcel);
    }

    public CompletableFuture<PageResult<Parcel>> getParcelsByReceiver(UUID receiver, Page page) {
        return this.parcelService.getByReceiver(receiver, page);
    }

    public CompletableFuture<Optional<List<Parcel>>> getParcelsBySender(UUID sender) {
        return this.parcelService.getBySender(sender);
    }

    public CompletableFuture<Optional<User>> getUser(UUID userUuid) {
        return this.userManager.get(userUuid);
    }

    public CompletableFuture<PageResult<User>> getUsers(Page page) {
        return this.userManager.getPage(page);
    }

    public CompletableFuture<Optional<Locker>> getLocker(UUID lockerUuid) {
        return this.lockerManager.get(lockerUuid);
    }

    public CompletableFuture<PageResult<Locker>> getLockerPage(Page page) {
        return this.lockerManager.get(page);
    }

    public CompletableFuture<ItemStorage> getItemStorage(UUID owner) {
        return this.itemStorageManager.get(owner)
            .thenApply(optional -> optional.orElse(new ItemStorage(owner, List.of())));
    }

    public void saveItemStorage(UUID player, List<ItemStack> items) {
        this.itemStorageManager.create(player, items);
    }

    public void deleteItemStorage(UUID owner) {
        this.itemStorageManager.delete(owner);
    }
}
