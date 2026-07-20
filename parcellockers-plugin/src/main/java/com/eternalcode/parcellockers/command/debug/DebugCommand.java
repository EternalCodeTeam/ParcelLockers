package com.eternalcode.parcellockers.command.debug;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.multification.notice.Notice;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Sender;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;

@Command(name = "parcel debug")
@Permission("parcellockers.debug")
public class DebugCommand {

    private final ParcelService parcelService;
    private final LockerManager lockerManager;
    private final ItemStorageManager itemStorageManager;
    private final ParcelContentManager contentManager;
    private final NoticeService noticeService;
    private final DeliveryManager deliveryManager;

    public DebugCommand(
        ParcelService parcelService,
        LockerManager lockerManager,
        ItemStorageManager itemStorageManager,
        ParcelContentManager contentManager,
        NoticeService noticeService, DeliveryManager deliveryManager
    ) {
        this.parcelService = parcelService;
        this.lockerManager = lockerManager;
        this.itemStorageManager = itemStorageManager;
        this.contentManager = contentManager;
        this.noticeService = noticeService;
        this.deliveryManager = deliveryManager;
    }

    @Execute(name = "delete parcels")
    void deleteParcels(@Sender CommandSender sender) {
        this.parcelService.deleteAll(sender, this.noticeService);
    }

    @Execute(name = "delete lockers")
    void deleteLockers(@Sender CommandSender sender) {
        this.lockerManager.deleteAll(sender, this.noticeService);
    }

    @Execute(name = "delete itemstorages")
    void deleteItemStorages(@Sender CommandSender sender) {
        this.itemStorageManager.deleteAll(sender, this.noticeService);
    }

    @Execute(name = "delete items")
    void deleteItems(@Sender CommandSender sender) {
        this.contentManager.deleteAll(sender, this.noticeService);
    }

    @Execute(name = "delete delivieries")
    void deleteDeliveries(@Sender CommandSender sender) {
        this.deliveryManager.deleteAll(sender, this.noticeService);
    }

    @Execute(name = "delete all")
    void deleteAll(@Sender CommandSender sender) {
        this.deleteItemStorages(sender);
        this.deleteDeliveries(sender);
        this.deleteLockers(sender);
        this.deleteParcels(sender);
        this.deleteItems(sender);
    }

    @Execute(name = "getrandomitem")
    void getRandomItem(@Sender Player player, @Arg int stacks) {
        if (stacks <= 0 || stacks > 36) {
            this.noticeService.player(player.getUniqueId(), messages -> Notice.chat("&cInvalid number of stacks. Must be between 1 and 36."));
            return;
        }

        Registry<ItemType> itemTypeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM);
        List<ItemType> types = itemTypeRegistry.keyStream().map(itemTypeRegistry::get).toList();

        if (types.isEmpty()) { // should never happen
            this.noticeService.player(player.getUniqueId(), messages -> Notice.chat("&cNo valid item materials found."));
            return;
        }

        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < stacks; i++) {
            ItemType randomItem = types.get(random.nextInt(types.size()));
            int randomAmount = Math.min(random.nextInt(64) + 1, randomItem.getMaxStackSize());
            ItemUtil.giveItem(player, randomItem.createItemStack(randomAmount));
        }
    }
}
