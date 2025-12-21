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

        // Save parcel first, then content. If content fails, delete parcel (transactional behavior)
        return this.parcelRepository.save(parcel)
            .thenCompose(unused -> this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items))
                .thenApply(contentSaved -> {
                    this.cache(parcel);
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
                    return true;
                })
                .exceptionally(contentError -> {
                    // Rollback: delete parcel if content save failed
                    this.parcelRepository.delete(parcel.uuid());
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                    throw new ParcelOperationException("Failed to save parcel content, rolled back parcel", contentError);
                })
            )
            .exceptionally(throwable -> {
                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                throw new ParcelOperationException("Failed to save parcel", throwable);
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
        return this.parcelContentRepository.fetch(parcel.uuid()).thenCompose(optional -> {
            if (optional.isEmpty()) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                return CompletableFuture.completedFuture(null);
            }

            List<ItemStack> items = optional.get().items();
            if (items.size() > freeSlotsInInventory(player)) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
                return CompletableFuture.completedFuture(null);
            }

            // Give items first
            items.forEach(item -> this.scheduler.run(() -> ItemUtil.giveItem(player, item)));

            // Then delete from database, only invalidate cache if successful
            return this.parcelRepository.delete(parcel)
                .thenCompose(deleted -> this.parcelContentRepository.delete(parcel.uuid())
                    .thenAccept(contentDeleted -> {
                        if (deleted > 0 && contentDeleted > 0) {
                            this.invalidate(parcel);
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.collected);
                        } else {
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                        }
                    })
                )
                .exceptionally(throwable -> {
                    this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                    return null;
                });
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

        // Use getIfPresent to avoid creating empty entries
        List<Parcel> bySender = this.parcelsBySender.getIfPresent(parcel.sender());
        if (bySender != null) {
            bySender.remove(parcel);
            if (bySender.isEmpty()) {
                this.parcelsBySender.invalidate(parcel.sender());
            } else {
                this.parcelsBySender.put(parcel.sender(), bySender);
            }
        }

        List<Parcel> byReceiver = this.parcelsByReceiver.getIfPresent(parcel.receiver());
        if (byReceiver != null) {
            byReceiver.remove(parcel);
            if (byReceiver.isEmpty()) {
                this.parcelsByReceiver.invalidate(parcel.receiver());
            } else {
                this.parcelsByReceiver.put(parcel.receiver(), byReceiver);
            }
        }
    }
}
