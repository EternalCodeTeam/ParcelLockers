package com.eternalcode.parcellockers.command.debug;

import com.eternalcode.multification.notice.Notice;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
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

    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelContentRepository contentRepository;
    private final NoticeService noticeService;

    public DebugCommand(
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        ItemStorageRepository itemStorageRepository,
        ParcelContentRepository contentRepository,
        NoticeService noticeService
    ) {
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.itemStorageRepository = itemStorageRepository;
        this.contentRepository = contentRepository;
        this.noticeService = noticeService;
    }

    @Execute(name = "delete parcels")
    void deleteParcels(@Sender CommandSender sender) {
        this.parcelRepository.removeAll().exceptionally(throwable -> {
            this.noticeService.create()
                .notice(Notice.chat("&4Failed to delete parcels"))
                .viewer(sender)
                .send();
            return null;
        }).thenRun(() -> this.noticeService.create()
            .notice(Notice.chat("&cParcels deleted"))
            .viewer(sender)
            .send());
    }

    @Execute(name = "delete lockers")
    void deleteLockers(@Sender CommandSender sender) {
        this.lockerRepository.deleteAll().exceptionally(throwable -> {
            this.noticeService.create()
                .notice(Notice.chat("&4Failed to delete lockers"))
                .viewer(sender)
                .send();
            return null;
        }).thenRun(() -> this.noticeService.create()
            .notice(Notice.chat("&cLockers deleted"))
            .viewer(sender)
            .send());
    }

    @Execute(name = "delete itemstorages")
    void deleteItemStorages(@Sender CommandSender sender) {
        this.itemStorageRepository.deleteAll()
            .exceptionally(throwable -> {
                this.noticeService.create()
                    .notice(Notice.chat("&4Failed to delete item storages"))
                    .viewer(sender)
                    .send();
                return null;
            })
            .thenRun(() -> this.noticeService.create()
                .notice(Notice.chat("&cItem storages deleted"))
                .viewer(sender)
                .send());
    }

    @Execute(name = "delete items")
    void deleteItems(@Sender CommandSender sender) {
        this.contentRepository.deleteAll().exceptionally(throwable -> {
            this.noticeService.create()
                .notice(Notice.chat("&4Failed to delete parcel contents"))
                .viewer(sender)
                .send();
            return null;
        }).thenRun(() -> this.noticeService.create()
            .notice(Notice.chat("&cParcel contents deleted"))
            .viewer(sender)
            .send());
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
            player.getInventory().addItem(itemStack);
        }
    }
}
