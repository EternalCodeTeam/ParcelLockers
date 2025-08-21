package com.eternalcode.parcellockers.parcel;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask;
import com.eternalcode.parcellockers.shared.ParcelLockersException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelServiceImpl implements ParcelService {

    private final PluginConfig config;
    private final NoticeService noticeService;
    private final ParcelRepository parcelRepository;
    private final DeliveryRepository deliveryRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;

    public ParcelServiceImpl(
        PluginConfig config,
        NoticeService noticeService,
        ParcelRepository parcelRepository,
        DeliveryRepository deliveryRepository,
        ParcelContentRepository parcelContentRepository,
        Scheduler scheduler
    ) {
        this.config = config;
        this.noticeService = noticeService;
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

        this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items)).handle((content, throwable) -> {
            if (throwable != null) {
                this.noticeService.create()
                    .notice(messages -> messages.parcelFailedToSend)
                    .player(sender.getUniqueId())
                    .send();

                throw new ParcelLockersException("Failed to save parcel content", throwable);
            }

            this.noticeService.create()
                .notice(messages -> messages.parcelSent)
                .player(sender.getUniqueId())
                .send();

            Delivery delivery = new Delivery(parcel.uuid(), Instant.now().plus(delay));
            ParcelSendTask task = new ParcelSendTask(parcel, delivery, parcelRepository, deliveryRepository, config);

            this.scheduler.runLaterAsync(task, delay);

            return true;
        });
        return true;
    }

    @Override
    public void remove(CommandSender sender, Parcel parcel) {
        this.parcelRepository.remove(parcel)
            .thenAccept(v ->
                this.noticeService.create()
                    .notice(messages -> messages.parcelSuccessfullyDeleted)
                    .viewer(sender)
                    .send())
            .exceptionally(throwable -> {
                this.noticeService.create()
                    .notice(messages -> messages.failedToDeleteParcel)
                    .viewer(sender)
                    .send();
                return null;
            });
    }

    @Override
    public void collect(Player player, Parcel parcel) {
        this.parcelContentRepository.find(parcel.uuid()).thenAccept(optional -> {
            if (optional.isEmpty()) {
                this.noticeService.create()
                    .notice(messages -> messages.failedToCollectParcel)
                    .player(player.getUniqueId())
                    .send();
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                this.noticeService.create()
                    .notice(messages -> messages.notEnoughInventorySpace)
                    .player(player.getUniqueId())
                    .send();
                return;
            }

            items.forEach(item -> this.scheduler.run(() -> ItemUtil.giveItem(player, item)));

            this.parcelRepository.remove(parcel)
                .thenAccept(v -> this.parcelContentRepository.delete(optional.get().uniqueId()))
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        this.noticeService.create()
                            .notice(messages -> messages.failedToCollectParcel)
                            .player(player.getUniqueId())
                            .send();
                        return;
                    }
                    this.noticeService.create()
                        .notice(messages -> messages.parcelSuccessfullyCollected)
                        .player(player.getUniqueId())
                        .send();
                });
        });
    }
}
