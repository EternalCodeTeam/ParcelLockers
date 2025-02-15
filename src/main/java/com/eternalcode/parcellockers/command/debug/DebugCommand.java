package com.eternalcode.parcellockers.command.debug;

import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import org.bukkit.entity.Player;

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

    @Execute(name = "deleteParcels", aliases = { "dp", "clearParcels", "purgeParcels" })
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

    @Execute(name = "deleteLockers", aliases = { "dl", "clearLockers", "purgeLockers" })
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

    @Execute(name = "deleteItemStorages", aliases = { "dis", "clearItemStorages", "purgeItemStorages" })
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

    @Execute(name = "deleteParcelContents", aliases = { "dpc", "clearParcelContents", "purgeParcelContents" })
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

}
