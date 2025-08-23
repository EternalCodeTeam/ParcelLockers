package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserManager;
import com.eternalcode.parcellockers.user.repository.UserPageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiManager {

    private final ParcelRepository parcelRepository;
    private final ParcelService parcelService;
    private final LockerRepository lockerRepository;
    private final DeliveryRepository deliveryRepository;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelContentRepository contentRepository;
    private final UserManager userManager;
    private final LockerManager lockerManager;

    public GuiManager(
        ParcelRepository parcelRepository, ParcelService parcelService,
        LockerRepository lockerRepository,
        DeliveryRepository deliveryRepository,
        ItemStorageRepository itemStorageRepository,
        ParcelContentRepository contentRepository,
        UserManager userManager, LockerManager lockerManager
    ) {
        this.parcelRepository = parcelRepository;
        this.parcelService = parcelService;
        this.lockerRepository = lockerRepository;
        this.deliveryRepository = deliveryRepository;
        this.itemStorageRepository = itemStorageRepository;
        this.contentRepository = contentRepository;
        this.userManager = userManager;
        this.lockerManager = lockerManager;
    }

    public CompletableFuture<PageResult<Parcel>> getParcelByReceiver(UUID receiver, Page page) {
        return this.parcelRepository.findByReceiver(receiver, page);
    }

    public CompletableFuture<Optional<List<Parcel>>> getParcelBySender(UUID sender) {
        return this.parcelRepository.findBySender(sender);
    }

    public CompletableFuture<Optional<User>> getUser(UUID userUuid) {
        return this.userManager.get(userUuid);
    }

    public CompletableFuture<UserPageResult> getUserPage(Page page) {
        return this.userManager.getPage(page);
    }

    public CompletableFuture<Optional<Locker>> getLocker(UUID lockerUuid) {
        return this.lockerManager.get(lockerUuid);
    }

    public CompletableFuture<PageResult<Locker>> getLockerPage(Page page) {
        return this.lockerRepository.findPage(page);
    }

    public CompletableFuture<Optional<ItemStorage>> getItemStorage(UUID owner) {
        return this.itemStorageRepository.find(owner);
    }

    public boolean sendParcel(Player sender, Parcel parcel, List<ItemStack> items) {
        return this.parcelService.send(sender, parcel, items);
    }

    public void collectParcel(Player player, Parcel parcel) {
        this.parcelService.collect(player, parcel);
    }

    public CompletableFuture<Void> saveItemStorage(ItemStorage itemStorage) {
        return this.itemStorageRepository.save(itemStorage);
    }

    public CompletableFuture<Integer> deleteItemStorage(UUID owner) {
        return this.itemStorageRepository.delete(owner);
    }
}
