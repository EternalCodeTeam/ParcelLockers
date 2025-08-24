package com.eternalcode.parcellockers.parcel;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.ParcelLockersException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    private final Cache<UUID, Parcel> parcelsByUuid = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build();

    private final Multimap<UUID, Parcel> parcelsBySender = HashMultimap.create();
    private final Multimap<UUID, Parcel> parcelsByReceiver = HashMultimap.create();

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
    public void send(Player sender, Parcel parcel, List<ItemStack> items) {
        this.parcelRepository.save(parcel).handle((v, throwable) -> {
            if (throwable != null) {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.cannotSend)
                    .player(sender.getUniqueId())
                    .send();
                throw new ParcelLockersException("Failed to save parcel", throwable);
            }

            this.noticeService.create()
                .notice(messages -> messages.parcel.sent)
                .player(sender.getUniqueId())
                .send();
            return null;
        });
    }

    @Override
    public void update(Parcel updated) {
        this.parcelRepository.update(updated);
        this.cache(updated);
    }

    @Override
    public void delete(CommandSender sender, Parcel parcel) {
        this.parcelRepository.delete(parcel)
            .thenAccept(v -> {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.deleted)
                    .viewer(sender)
                    .send();
                this.invalidate(parcel);
            })
            .exceptionally(throwable -> {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.cannotDelete)
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
                    .notice(messages -> messages.parcel.cannotCollect)
                    .player(player.getUniqueId())
                    .send();
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.noInventorySpace)
                    .player(player.getUniqueId())
                    .send();
                return;
            }

            items.forEach(item -> this.scheduler.run(() -> ItemUtil.giveItem(player, item)));


            // TODO: Do not delete, archive instead
            this.invalidate(parcel);
            this.parcelRepository.delete(parcel);
            this.parcelContentRepository.delete(parcel.uuid());

            this.noticeService.create()
                .notice(messages -> messages.parcel.collected)
                .player(player.getUniqueId())
                .send();
        });
    }

    @Override
    public CompletableFuture<Optional<Parcel>> get(UUID uuid) {
        Parcel cached = this.parcelsByUuid.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.parcelRepository.findById(uuid).thenApply(optional -> {
            optional.ifPresent(this::cache);
            return optional;
        });
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> getBySender(UUID sender) {
        List<Parcel> cached = List.copyOf(this.parcelsBySender.get(sender));
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.parcelRepository.findBySender(sender).thenApply(optional -> {
            optional.ifPresent(parcels -> parcels.forEach(this::cache));
            return optional;
        });
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> getByReceiver(UUID receiver) {
        List<Parcel> cached = List.copyOf(this.parcelsByReceiver.get(receiver));
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.parcelRepository.findByReceiver(receiver).thenApply(optional -> {
            optional.ifPresent(parcels -> parcels.forEach(this::cache));
            return optional;
        });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page) {
        return this.parcelRepository.findByReceiver(receiver, page).thenApply(result -> {
            result.items().forEach(this::cache);
            return result;
        });
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> getAll() {
        List<Parcel> cached = List.copyOf(this.parcelsByUuid.asMap().values());
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return this.parcelRepository.findAll().thenApply(optional -> {
            optional.ifPresent(parcels -> parcels.forEach(this::cache));
            return optional;
        });
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
        this.parcelsByUuid.invalidate(uuid);
        return this.parcelRepository.delete(uuid).thenApply(deleted -> {
            if (deleted > 0) {
                Parcel cached = this.parcelsByUuid.getIfPresent(uuid);
                if (cached != null) {
                    this.invalidate(cached);
                }
            }
            return deleted;
        });
    }

    @Override
    public CompletableFuture<Integer> delete(Parcel parcel) {
        return this.delete(parcel.uuid());
    }

    @Override
    public void deleteAll(CommandSender sender, NoticeService noticeService) {
        this.parcelRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedParcels)
                .viewer(sender)
                .placeholder("{COUNT}", deleted.toString())
                .send();

            this.parcelsByUuid.invalidateAll();
            this.parcelsBySender.clear();
            this.parcelsByReceiver.clear();
        });
    }

    private void cache(Parcel parcel) {
        this.parcelsByUuid.put(parcel.uuid(), parcel);
        this.parcelsBySender.put(parcel.sender(), parcel);
        this.parcelsByReceiver.put(parcel.receiver(), parcel);
    }

    private void invalidate(Parcel parcel) {
        this.parcelsByUuid.invalidate(parcel.uuid());
        this.parcelsBySender.remove(parcel.sender(), parcel);
        this.parcelsByReceiver.remove(parcel.receiver(), parcel);
    }
}
