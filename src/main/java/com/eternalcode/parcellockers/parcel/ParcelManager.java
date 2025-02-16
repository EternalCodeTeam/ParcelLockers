package com.eternalcode.parcellockers.parcel;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

public class ParcelManager {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;

    public ParcelManager(PluginConfiguration config, NotificationAnnouncer announcer, ParcelRepository parcelRepository, ParcelContentRepository parcelContentRepository, Scheduler scheduler) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.scheduler = scheduler;
    }

    public void createParcel(CommandSender sender, Parcel parcel) {
        this.parcelRepository.save(parcel)
            .whenComplete(SentryExceptionHandler.handler()
            .andThen((v, throwable) -> {
                if (throwable != null) {
                    this.announcer.sendMessage(sender, this.config.messages.failedToCreateParcel);
                    return;
                }
                this.announcer.sendMessage(sender, this.config.messages.parcelSuccessfullyCreated);
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
        this.parcelContentRepository.findByUUID(parcel.uuid()).thenAccept(optional -> {
            optional.ifPresent(content -> {
                List<ItemStack> items = content.items();
                if (items.size() > freeSlotsInInventory(player)) {
                    player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5F, 1);
                    this.announcer.sendMessage(player, this.config.messages.notEnoughInventorySpace);
                    return;
                }

                items.forEach(item ->
                    this.scheduler.run(() -> ItemUtil.giveItem(player, item))
                );
                this.parcelRepository.remove(parcel)
                    .thenCompose(v -> this.parcelContentRepository.remove(content.uniqueId()))
                    .whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                            if (throwable != null) {
                                this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                                return;
                            }
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1);
                            this.announcer.sendMessage(player, this.config.messages.parcelSuccessfullyCollected);
                        }
                    ));
            });
        });
    }
}
