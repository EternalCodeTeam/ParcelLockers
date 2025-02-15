package com.eternalcode.parcellockers.command.debug;

import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

@Command(name = "parcel debug")
@Permission("parcellockers.debug")
public class DebugCommand {

    private static final Random RANDOM = new Random();

    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final ItemStorageRepository itemStorageRepository;
    private final ParcelContentRepository contentRepository;
    private final NotificationAnnouncer announcer;

    public DebugCommand(ParcelRepository parcelRepository, LockerRepository lockerRepository, ItemStorageRepository itemStorageRepository, ParcelContentRepository contentRepository, NotificationAnnouncer announcer) {
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.itemStorageRepository = itemStorageRepository;
        this.contentRepository = contentRepository;
        this.announcer = announcer;
    }

    @Execute(name = "deleteparcels")
    void deleteParcels(@Context Player player) {
        this.parcelRepository.removeAll().thenAccept(v -> {
        }).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(player, "&cFailed to delete parcels");
                return;
            }
            this.announcer.sendMessage(player, "&cParcels deleted");
        });
    }

    @Execute(name = "deletelockers")
    void deleteLockers(@Context Player player) {
        this.lockerRepository.removeAll().thenAccept(v -> {
        }).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(player, "&cFailed to delete lockers");
                return;
            }
            this.announcer.sendMessage(player, "&cLockers deleted");
        });
    }

    @Execute(name = "deleteitemstorages")
    void deleteItemStorages(@Context Player player) {
        this.itemStorageRepository.removeAll().thenAccept(v -> {
        }).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(player, "&cFailed to delete item storages");
                return;
            }
            this.announcer.sendMessage(player, "&cItem storages deleted");
        });
    }

    @Execute(name = "deleteparcelcontents")
    void deleteParcelContents(@Context Player player) {
        this.contentRepository.removeAll().thenAccept(v -> {
        }).whenComplete((v, throwable) -> {
            if (throwable != null) {
                this.announcer.sendMessage(player, "&cFailed to delete parcel contents");
                return;
            }
            this.announcer.sendMessage(player, "&cParcel contents deleted");
        });
    }

    @Execute(name = "deleteall")
    void deleteAll(@Context Player player) {
        this.deleteItemStorages(player);
        this.deleteLockers(player);
        this.deleteParcels(player);
        this.deleteParcelContents(player);
    }

    @Execute(name = "getrandomitem")
    void getRandomItem(@Context Player player, @Arg int stacks) {
        Material[] materials = Material.values();
        if (stacks <= 0 || stacks > 36) {
            this.announcer.sendMessage(player, "&cPlease request between 1 and 36 stacks");
            return;
        }

        for (int i = 0; i < stacks; i++) {
            Material randomMaterial = materials[RANDOM.nextInt(materials.length)];

            if (!randomMaterial.isItem()) {
                i--;
                continue;
            }

            int randomAmount = RANDOM.nextInt(64) + 1;
            if (randomAmount > randomMaterial.getMaxStackSize()) {
                randomAmount = randomMaterial.getMaxStackSize();
            }
            ItemStack itemStack = new ItemStack(randomMaterial, randomAmount);
            player.getInventory().addItem(itemStack);
        }
    }

}
