package com.eternalcode.parcellockers.parcel;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.shared.ParcelLockersException;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelServiceImpl implements ParcelService {

    private final PluginConfiguration config;
    private final NotificationAnnouncer announcer;
    private final ParcelRepository parcelRepository;
    private final DeliveryRepository deliveryRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;

    public ParcelServiceImpl(
        PluginConfiguration config,
        NotificationAnnouncer announcer,
        ParcelRepository parcelRepository,
        DeliveryRepository deliveryRepository,
        ParcelContentRepository parcelContentRepository,
        Scheduler scheduler
    ) {
        this.config = config;
        this.announcer = announcer;
        this.parcelRepository = parcelRepository;
        this.deliveryRepository = deliveryRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.scheduler = scheduler;
    }

    @Override
    public boolean send(Player sender, Parcel parcel, List<ItemStack> items) {
        Duration delay = parcel.priority()
            ? config.settings.priorityParcelSendDuration
            : config.settings.parcelSendDuration;

        this.parcelRepository.save(parcel);

        parcelContentRepository.save(new ParcelContent(parcel.uuid(), items)).handle((content, throwable) -> {
            if (throwable != null) {
                announcer.sendMessage(sender, config.messages.parcelFailedToSend);
                throw new ParcelLockersException("Failed to save parcel content", throwable);
            }
            announcer.sendMessage(sender, config.messages.parcelSent);

            Delivery delivery = new Delivery(parcel.uuid(), Instant.now().plus(delay));
            ParcelSendTask task = new ParcelSendTask(parcel, delivery, parcelRepository, deliveryRepository, config);
            scheduler.runLaterAsync(task, delay);
            return true;
        });
        return true;
    }

    @Override
    public void remove(CommandSender sender, Parcel parcel) {
        this.parcelRepository.remove(parcel)
            .thenAccept(v -> announcer.sendMessage(sender, config.messages.parcelSuccessfullyDeleted))
            .whenComplete((v, throwable) -> {
                if (throwable != null) {
                    announcer.sendMessage(sender, config.messages.failedToDeleteParcel);
                }
            });
    }

    @Override
    public void collect(Player player, Parcel parcel) {
        this.parcelContentRepository.find(parcel.uuid()).thenAccept(optional -> {
            if (optional.isEmpty()) {
                playErrorSound(player);
                this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                playErrorSound(player);
                this.announcer.sendMessage(player, this.config.messages.notEnoughInventorySpace);
                return;
            }

            items.forEach(item -> this.scheduler.run(() -> ItemUtil.giveItem(player, item)));

            this.parcelRepository.remove(parcel)
                .thenCompose(v -> this.parcelContentRepository.delete(optional.get().uniqueId()))
                .whenComplete(SentryExceptionHandler.handler().andThen((v, throwable) -> {
                    if (throwable != null) {
                        this.announcer.sendMessage(player, this.config.messages.failedToCollectParcel);
                    } else {
                        playSuccessSound(player);
                        this.announcer.sendMessage(player, this.config.messages.parcelSuccessfullyCollected);
                    }
                }));
        });
    }

    private void playErrorSound(Player player) {
        player.playSound(
            player.getLocation(),
            this.config.settings.errorSound,
            this.config.settings.errorSoundVolume,
            this.config.settings.errorSoundPitch
        );
    }

    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1);
    }
}
