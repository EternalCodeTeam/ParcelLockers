package com.eternalcode.parcellockers.command.debug;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.multification.notice.Notice;
import com.eternalcode.parcellockers.content.ParcelContentManager;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.itemstorage.ItemStorageManager;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.ParcelService;
import com.eternalcode.parcellockers.user.UserManager;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Sender;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

// TODO move notices to MessagesConfig

@Command(name = "parcel debug")
@Permission("parcellockers.debug")
public class DebugCommand {

    private final ParcelService parcelService;
    private final LockerManager lockerManager;
    private final ItemStorageManager itemStorageManager;
    private final ParcelContentManager contentManager;
    private final DeliveryManager deliveryManager;
    private final UserManager userManager;
    private final NoticeService noticeService;

    public DebugCommand(
        ParcelService parcelService,
        LockerManager lockerManager,
        ItemStorageManager itemStorageManager,
        ParcelContentManager contentManager,
        DeliveryManager deliveryManager,
        UserManager userManager,
        NoticeService noticeService
    ) {
        this.parcelService = parcelService;
        this.lockerManager = lockerManager;
        this.itemStorageManager = itemStorageManager;
        this.contentManager = contentManager;
        this.deliveryManager = deliveryManager;
        this.userManager = userManager;
        this.noticeService = noticeService;
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

    @Execute(name = "delete all")
    void deleteAll(@Sender CommandSender sender) {
        this.deleteItemStorages(sender);
        this.deleteLockers(sender);
        this.deleteParcels(sender);
        this.deleteItems(sender);
    }

    @Execute(name = "getrandomitem")
    void getRandomItem(@Sender Player player, @Arg int stacks) {
        if (stacks <= 0 || stacks > 36) {
            this.noticeService.create()
                .notice(Notice.chat("&cInvalid number of stacks. Must be between 1 and 36."))
                .player(player.getUniqueId())
                .send();
            return;
        }

        List<Material> itemMaterials = Arrays.stream(Material.values()).filter(Material::isItem).toList();

        if (itemMaterials.isEmpty()) {
            this.noticeService.create()
                .notice(Notice.chat("&cNo valid item materials found."))
                .player(player.getUniqueId())
                .send();
            return;
        }

        Random random = ThreadLocalRandom.current();

        // give player random items
        for (int i = 0; i < stacks; i++) {
            Material randomMaterial = itemMaterials.get(random.nextInt(itemMaterials.size()));
            int randomAmount = Math.min(random.nextInt(64) + 1, randomMaterial.getMaxStackSize());

            ItemStack itemStack = new ItemStack(randomMaterial, randomAmount);
            ItemUtil.giveItem(player, itemStack);
        }
    }
}
