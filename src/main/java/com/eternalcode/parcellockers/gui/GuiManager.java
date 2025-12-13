package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiManager {

    private final ParcelService parcelService;
    private final LockerManager lockerManager;
    private final UserManager userManager;
    private final ItemStorageManager itemStorageManager;
    private final ParcelDispatchService parcelDispatchService;

    public GuiManager(
        ParcelService parcelService,
        LockerManager lockerManager,
        UserManager userManager,
        ItemStorageManager itemStorageManager,
        ParcelDispatchService parcelDispatchService
    ) {
        this.parcelService = parcelService;
        this.lockerManager = lockerManager;
        this.userManager = userManager;
        this.itemStorageManager = itemStorageManager;
        this.parcelDispatchService = parcelDispatchService;
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

    public CompletableFuture<Boolean> deleteItemStorage(UUID owner) {
        return this.itemStorageManager.delete(owner);
    }
}
