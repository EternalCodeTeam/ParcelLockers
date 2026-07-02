package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiManager {

    private final ParcelService parcelService;
    private final LockerManager lockerManager;
    private final UserManager userManager;
    private final ItemStorageManager itemStorageManager;
    private final ParcelDispatchService parcelDispatchService;
    private final ParcelContentManager parcelContentManager;
    private final DeliveryManager deliveryManager;
    private final boolean allowCollectingFromAnyLocker;

    public GuiManager(
        ParcelService parcelService,
        LockerManager lockerManager,
        UserManager userManager,
        ItemStorageManager itemStorageManager,
        ParcelDispatchService parcelDispatchService,
        ParcelContentManager parcelContentManager,
        DeliveryManager deliveryManager,
        boolean allowCollectingFromAnyLocker
    ) {
        this.parcelService = parcelService;
        this.lockerManager = lockerManager;
        this.userManager = userManager;
        this.itemStorageManager = itemStorageManager;
        this.parcelDispatchService = parcelDispatchService;
        this.parcelContentManager = parcelContentManager;
        this.deliveryManager = deliveryManager;
        this.allowCollectingFromAnyLocker = allowCollectingFromAnyLocker;
    }

    /**
     * Returns the delivered parcels the receiver may collect at the locker they are interacting with.
     * When {@code allowCollectingFromAnyLocker} is enabled the locker restriction is dropped.
     *
     * <p>{@code getCollectible} treats a {@code null} destination locker as "collect from any locker",
     * so when the restriction is active a concrete {@code currentLocker} is required — passing
     * {@code null} here would silently drop the restriction and let the receiver collect from any locker.
     */
    public CompletableFuture<PageResult<Parcel>> getCollectibleParcels(UUID receiver, UUID currentLocker, Page page) {
        if (this.allowCollectingFromAnyLocker) {
            return this.parcelService.getCollectible(receiver, null, page);
        }
        Objects.requireNonNull(currentLocker,
            "currentLocker must not be null when collection is restricted to the destination locker");
        return this.parcelService.getCollectible(receiver, currentLocker, page);
    }

    public void sendParcel(Player sender, Parcel parcel, List<ItemStack> items) {
        this.parcelDispatchService.dispatch(sender, parcel, items);
    }

    public void collectParcel(Player player, Parcel parcel) {
        this.parcelService.collect(player, parcel);
    }

    public CompletableFuture<PageResult<Parcel>> getParcelsByReceiver(UUID receiver, Page page) {
        return this.parcelService.getByReceiver(receiver, page);
    }

    public CompletableFuture<PageResult<Parcel>> getParcelsBySender(UUID sender, Page page) {
        return this.parcelService.getBySender(sender, page);
    }

    public CompletableFuture<PageResult<Parcel>> getAllParcels(Page page) {
        return this.parcelService.getAll(page);
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

    public CompletableFuture<ItemStorage> saveItemStorage(UUID player, List<ItemStack> items) {
        return this.itemStorageManager.create(player, items);
    }

    public CompletableFuture<Boolean> deleteItemStorage(UUID owner) {
        return this.itemStorageManager.delete(owner);
    }

    public CompletableFuture<Optional<ParcelContent>> getParcelContent(UUID parcelId) {
        return this.parcelContentManager.get(parcelId);
    }

    public CompletableFuture<ParcelContent> updateParcelContent(UUID parcelId, List<ItemStack> items) {
        return this.parcelContentManager.update(parcelId, items);
    }

    public CompletableFuture<Optional<Delivery>> getDelivery(UUID parcelId) {
        return this.deliveryManager.get(parcelId);
    }

    public CompletableFuture<Locker> renameLocker(UUID lockerUuid, String newName) {
        return this.lockerManager.rename(lockerUuid, newName);
    }

    public CompletableFuture<Void> deleteLocker(UUID lockerUuid, UUID actor) {
        return this.lockerManager.delete(lockerUuid, actor);
    }

    public CompletableFuture<Optional<Parcel>> getParcel(UUID uuid) {
        return this.parcelService.get(uuid);
    }

    public CompletableFuture<Boolean> deleteParcel(Parcel parcel) {
        return this.parcelService.delete(parcel);
    }

    public CompletableFuture<Void> deleteAllParcels(CommandSender sender, NoticeService noticeService) {
        return this.parcelService.deleteAll(sender, noticeService);
    }

    public CompletableFuture<Void> deleteAllLockers(CommandSender sender, NoticeService noticeService) {
        return this.lockerManager.deleteAll(sender, noticeService);
    }
}
