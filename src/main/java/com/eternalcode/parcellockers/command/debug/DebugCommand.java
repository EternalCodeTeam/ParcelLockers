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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Command(name = "parcel debug")
@Permission("parcellockers.debug")
public class DebugCommand {

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
    void deleteParcels(@Context CommandSender sender) {
        this.parcelRepository.removeAll().exceptionally(throwable -> {
            this.announcer.sendMessage(sender, "&4Failed to delete parcels");
            return null;
        }).thenRun(() -> this.announcer.sendMessage(sender, "&cParcels deleted"));
    }

    @Execute(name = "deletelockers")
    void deleteLockers(@Context CommandSender sender) {
        this.lockerRepository.removeAll().exceptionally(throwable -> {
            this.announcer.sendMessage(sender, "&4Failed to delete lockers");
            return null;
        }).thenRun(() -> this.announcer.sendMessage(sender, "&cLockers deleted"));
    }

    @Execute(name = "deleteitemstorages")
    void deleteItemStorages(@Context CommandSender sender) {
        this.itemStorageRepository.removeAll().exceptionally(throwable -> {
            this.announcer.sendMessage(sender, "&4Failed to delete item storages");
            return null;
        }).thenRun(() -> this.announcer.sendMessage(sender, "&cItem storages deleted"));
    }

    @Execute(name = "deleteparcelcontents")
    void deleteParcelContents(@Context CommandSender sender) {
        this.contentRepository.removeAll().exceptionally(throwable -> {
            this.announcer.sendMessage(sender, "&4Failed to delete parcel contents");
            return null;
        }).thenRun(() -> this.announcer.sendMessage(sender, "&cParcel contents deleted"));
    }

    @Execute(name = "deleteall")
    void deleteAll(@Context CommandSender sender) {
        this.deleteItemStorages(sender);
        this.deleteLockers(sender);
        this.deleteParcels(sender);
        this.deleteParcelContents(sender);
    }

    @Execute(name = "getrandomitem")
    void getRandomItem(@Context Player player, @Arg int stacks) {
        if (stacks <= 0 || stacks > 36) {
            this.announcer.sendMessage(player, "&cPlease request between 1 and 36 stacks");
            return;
        }

        List<Material> itemMaterials = Arrays.stream(Material.values()).filter(Material::isItem).toList();

        if (itemMaterials.isEmpty()) {
            this.announcer.sendMessage(player, "&cNo valid items found.");
            return;
        }

        // Faster solution than Random#nextInt
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < stacks; i++) {
            Material randomMaterial = itemMaterials.get(random.nextInt(itemMaterials.size()));
            int randomAmount = Math.min(random.nextInt(64) + 1, randomMaterial.getMaxStackSize());

            ItemStack itemStack = new ItemStack(randomMaterial, randomAmount);
            player.getInventory().addItem(itemStack);
        }
    }

}
