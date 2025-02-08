package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import com.eternalcode.parcellockers.util.InventoryUtil;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ParcelManager {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;

    public ParcelManager(PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepository parcelRepository, ParcelContentRepository parcelContentRepository) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
    }

    public void createParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.save(parcel).thenAccept(v ->
            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyCreated)
        ).whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                if (throwable != null) {
                    this.announcer.sendMessage(sender, this.config.messages.failedToCreateParcel);
                }
            }
        ));
    }

    public void deleteParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.remove(parcel).thenAccept(v ->
            this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyDeleted)
        ).whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                if (throwable != null) {
                    this.announcer.sendMessage(sender, this.config.messages.failedToDeleteParcel);
                }
            }
        ));
    }

    public void collectParcel(Player player, Parcel parcel) {
        this.parcelContentRepository.find(parcel.uuid()).thenAccept(optional -> {
            optional.ifPresent(content -> {
                List<ItemStack> items = content.items();
                if (player.getInventory().getStorageContents().length > items.size()) {
                    player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1, 1);
                    this.announcer.sendMessage(player, this.config.messages.notEnoughInventorySpace);
                    return;
                }
                items.forEach(item -> InventoryUtil.addItem(player, item));
                this.parcelRepository.remove(parcel)
                    .whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                        if (throwable != null) {
                            this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                            return;
                        }
                        this.announcer.sendMessage(player, this.config.messages.parcelSuccessfullyCollected);
                    }
                ));
            });
        });
    }
}
