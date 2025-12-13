package com.eternalcode.parcellockers.parcel;

import static com.eternalcode.parcellockers.util.InventoryUtil.freeSlotsInInventory;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelServiceImpl implements ParcelService {

    private static final String PARCEL_FEE_BYPASS_PERMISSION = "parcellockers.fee.bypass";

    private final NoticeService noticeService;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Economy economy;

    private final Cache<UUID, Parcel> parcelsByUuid;
    private final Cache<UUID, List<Parcel>> parcelsBySender;
    private final Cache<UUID, List<Parcel>> parcelsByReceiver;

    public ParcelServiceImpl(
        NoticeService noticeService,
        ParcelRepository parcelRepository,
        ParcelContentRepository parcelContentRepository,
        Scheduler scheduler, PluginConfig config, Economy economy
    ) {
        this.noticeService = noticeService;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.scheduler = scheduler;
        this.config = config;
        this.economy = economy;

        this.parcelsByUuid = Caffeine.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

        this.parcelsBySender = Caffeine.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

        this.parcelsByReceiver = Caffeine.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

        this.cacheAll();
    }

    @Override
    public CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items) {
        if (!sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            double fee = switch (parcel.size()) {
                case SMALL -> this.config.settings.smallParcelFee;
                case MEDIUM -> this.config.settings.mediumParcelFee;
                case LARGE -> this.config.settings.largeParcelFee;
            };

            if (fee > 0) {
                boolean success = this.economy.withdrawPlayer(sender, fee).transactionSuccess();
                if (!success) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.insufficientFunds)
                        .player(sender.getUniqueId())
                        .placeholder("{AMOUNT}", String.format("%.2f", fee))
                        .send();
                    return CompletableFuture.completedFuture(false);
                }

                this.noticeService.create()
                    .notice(messages -> messages.parcel.feeWithdrawn)
                    .player(sender.getUniqueId())
                    .placeholder("{AMOUNT}", String.format("%.2f", fee))
                    .send();
            }
        }

        return this.parcelRepository.save(parcel).handle((unused, throwable) -> {
            if (throwable != null) {
                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                throw new ParcelOperationException("Failed to save parcel", throwable);
            }

            this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items));
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
            return true;
        });
    }

    @Override
    public CompletableFuture<Void> update(Parcel updated) {
        this.cache(updated);
        return this.parcelRepository.update(updated);
    }

    @Override
    public CompletableFuture<Void> delete(CommandSender sender, Parcel parcel) {
        return this.parcelRepository.delete(parcel)
            .thenAccept(unused -> {
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
    public CompletableFuture<Void> collect(Player player, Parcel parcel) {
        return this.parcelContentRepository.fetch(parcel.uuid()).thenAccept(optional -> {
            if (optional.isEmpty()) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                return;
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
                return;
            }

            items.forEach(item -> this.scheduler.run(() -> ItemUtil.giveItem(player, item)));


            // TODO in the future: Do not delete, archive instead
            this.invalidate(parcel);
            this.parcelRepository.delete(parcel);
            this.parcelContentRepository.delete(parcel.uuid());

            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.collected);
        });
    }

    @Override
    public CompletableFuture<Optional<Parcel>> get(UUID uuid) {
        Parcel cached = this.parcelsByUuid.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.parcelRepository.fetchById(uuid).thenApply(optional -> {
            optional.ifPresent(this::cache);
            return optional;
        });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getBySender(UUID receiver, Page page) {
        List<Parcel> cached = this.parcelsBySender.getIfPresent(receiver);

        if (cached != null) {
            int fromIndex = Math.min(page.getOffset(), cached.size());
            int toIndex = Math.min(page.getOffset() + page.getLimit(), cached.size());

            List<Parcel> pageItems = cached.subList(fromIndex, toIndex);
            boolean hasNextPage = toIndex < cached.size();

            return CompletableFuture.completedFuture(new PageResult<>(pageItems, hasNextPage));
        }

        return this.parcelRepository.fetchBySender(receiver, page)
            .thenApply(result -> {
                result.items().forEach(this::cache);
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page) {
        List<Parcel> cached = this.parcelsByReceiver.getIfPresent(receiver);

        if (cached != null) {
            int fromIndex = Math.min(page.getOffset(), cached.size());
            int toIndex = Math.min(page.getOffset() + page.getLimit(), cached.size());

            List<Parcel> pageItems = cached.subList(fromIndex, toIndex);
            boolean hasNextPage = toIndex < cached.size();

            return CompletableFuture.completedFuture(new PageResult<>(pageItems, hasNextPage));
        }

        return this.parcelRepository.fetchByReceiver(receiver, page)
            .thenApply(result -> {
                result.items().forEach(this::cache);
                return result;
            });
    }


    private void cacheAll() {
        this.parcelRepository.fetchAll()
            .thenAccept(optional -> optional.ifPresent(parcels -> parcels.forEach(this::cache)));
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
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
    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.parcelRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedParcels)
                .viewer(sender)
                .placeholder("{COUNT}", deleted.toString())
                .send();

            this.parcelsByUuid.invalidateAll();
            this.parcelsBySender.invalidateAll();
            this.parcelsByReceiver.invalidateAll();
        });
    }

    private void cache(Parcel parcel) {
        this.parcelsByUuid.put(parcel.uuid(), parcel);

        List<Parcel> bySender = this.parcelsBySender.get(parcel.sender(), k -> new ArrayList<>());
        bySender.add(parcel);
        this.parcelsBySender.put(parcel.sender(), bySender);

        List<Parcel> byReceiver = this.parcelsByReceiver.get(parcel.receiver(), k -> new ArrayList<>());
        byReceiver.add(parcel);
        this.parcelsByReceiver.put(parcel.receiver(), byReceiver);
    }


    private void invalidate(Parcel parcel) {
        this.parcelsByUuid.invalidate(parcel.uuid());

        List<Parcel> bySender = this.parcelsBySender.get(parcel.sender(), k -> new ArrayList<>());
        bySender.remove(parcel);
        this.parcelsBySender.put(parcel.sender(), bySender);

        List<Parcel> byReceiver = this.parcelsByReceiver.get(parcel.receiver(), k -> new ArrayList<>());
        byReceiver.remove(parcel);
        this.parcelsByReceiver.put(parcel.receiver(), byReceiver);
    }
}
