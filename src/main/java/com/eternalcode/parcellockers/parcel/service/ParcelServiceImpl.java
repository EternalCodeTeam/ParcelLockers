package com.eternalcode.parcellockers.parcel.service;

import static com.eternalcode.parcellockers.util.InventoryUtil.canHold;

import com.eternalcode.commons.bukkit.ItemUtil;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.event.ParcelCollectEvent;
import com.eternalcode.parcellockers.parcel.event.ParcelSendEvent;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelServiceImpl implements ParcelService {

    private static final String PARCEL_FEE_BYPASS_PERMISSION = "parcellockers.fee.bypass";
    private static final String PLACEHOLDER_AMOUNT = "{AMOUNT}";
    private static final String PLACEHOLDER_COUNT = "{COUNT}";

    private static final long CACHE_EXPIRE_HOURS = 3;
    private static final long CACHE_MAX_SIZE = 10_000;

    private final NoticeService noticeService;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final CollectedParcelRepository collectedParcelRepository;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Economy economy;
    private final Server server;

    private final Cache<UUID, Parcel> parcelsByUuid;

    public ParcelServiceImpl(
        NoticeService noticeService,
        ParcelRepository parcelRepository,
        ParcelContentRepository parcelContentRepository,
        CollectedParcelRepository collectedParcelRepository,
        Scheduler scheduler,
        PluginConfig config,
        Economy economy,
        Server server
    ) {
        this.noticeService = noticeService;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.collectedParcelRepository = collectedParcelRepository;
        this.scheduler = scheduler;
        this.config = config;
        this.economy = economy;
        this.server = server;

        this.parcelsByUuid = Caffeine.newBuilder()
            .expireAfterAccess(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
            .maximumSize(CACHE_MAX_SIZE)
            .build();
    }

    @Override
    public CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        Objects.requireNonNull(items, "Items list cannot be null");
        Preconditions.checkArgument(!items.isEmpty(), "Items list cannot be empty");

        ParcelSendEvent event = new ParcelSendEvent(parcel);
        this.server.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
            return CompletableFuture.completedFuture(false);
        }

        List<ItemStack> itemsCopy = items.stream()
            .map(ItemStack::clone)
            .toList();

        double chargedFee = 0;
        if (!sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            double fee = this.feeFor(parcel.size());

            if (fee > 0) {
                boolean success = this.economy.withdrawPlayer(sender, fee).transactionSuccess();
                String formattedFee = String.format("%.2f", fee);

                if (!success) {
                    this.noticeService.create()
                        .notice(messages -> messages.parcel.insufficientFunds)
                        .player(sender.getUniqueId())
                        .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                        .send();
                    return CompletableFuture.completedFuture(false);
                }

                chargedFee = fee;
                this.noticeService.create()
                    .notice(messages -> messages.parcel.feeWithdrawn)
                    .player(sender.getUniqueId())
                    .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                    .send();
            }
        }

        double refundableFee = chargedFee;
        return this.parcelRepository.save(parcel)
            .thenCompose(unused -> this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), itemsCopy))
                .thenApply(contentSaved -> {
                    this.parcelsByUuid.put(parcel.uuid(), parcel);
                    // The "sent" notice is issued by the dispatcher once the whole send succeeds, so it
                    // is not shown when a later step (e.g. clearing storage) fails and rolls back.
                    return true;
                })
                .exceptionallyCompose(contentError -> this.parcelRepository.delete(parcel.uuid())
                    .thenCompose(deleted -> CompletableFuture.failedFuture(new ParcelOperationException("Failed to save parcel content, rolled back parcel", contentError))))
            )
            .exceptionally(throwable -> {
                // Persistence failed after the fee was withdrawn - refund it so the player is not charged for a parcel that was never created.
                this.refundFee(sender, refundableFee);
                this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                throw new ParcelOperationException("Failed to save parcel", throwable);
            });
    }

    @Override
    public CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");

        if (!sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            this.refundFee(sender, this.feeFor(parcel.size()));
        }
        this.parcelsByUuid.invalidate(parcel.uuid());

        return this.parcelRepository.delete(parcel.uuid())
            .thenCompose(deleted -> this.parcelContentRepository.delete(parcel.uuid()))
            .thenApply(contentDeleted -> null);
    }

    private double feeFor(ParcelSize size) {
        return switch (size) {
            case SMALL -> this.config.settings.smallParcelFee;
            case MEDIUM -> this.config.settings.mediumParcelFee;
            case LARGE -> this.config.settings.largeParcelFee;
        };
    }

    private void refundFee(Player sender, double fee) {
        if (fee > 0) {
            this.economy.depositPlayer(sender, fee);
        }
    }

    @Override
    public CompletableFuture<Void> update(Parcel updated) {
        Objects.requireNonNull(updated, "Updated parcel cannot be null");
        this.parcelsByUuid.put(updated.uuid(), updated);
        return this.parcelRepository.update(updated);
    }

    @Override
    public CompletableFuture<Void> delete(CommandSender sender, Parcel parcel) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");

        return this.parcelRepository.delete(parcel)
            .thenAccept(unused -> {
                this.noticeService.create()
                    .notice(messages -> messages.parcel.deleted)
                    .viewer(sender)
                    .send();
                this.parcelsByUuid.invalidate(parcel.uuid());
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
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");

        // Fire ParcelCollectEvent
        ParcelCollectEvent event = new ParcelCollectEvent(parcel);
        this.server.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
            return CompletableFuture.completedFuture(null);
        }

        return this.parcelContentRepository.find(parcel.uuid()).thenCompose(optional -> {
            if (optional.isEmpty()) {
                this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                return CompletableFuture.completedFuture(null);
            }

            List<ItemStack> items = optional.get().items();
            CompletableFuture<Void> result = new CompletableFuture<>();

            // Re-check inventory space on the main thread (the previous async check was a TOCTOU),
            // then flip the status BEFORE handing the items back so the parcel cannot be collected
            // twice. The parcel and content rows are kept: they are the snapshot a later return is
            // validated against. The collected_parcels row is written first so that a successful
            // flip always has a collection timestamp; a stray row from a failed flip is ignored by
            // the purge task (it only purges parcels that are actually COLLECTED).
            this.scheduler.run(() -> {
                if (!canHold(player, items)) {
                    this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
                    result.complete(null);
                    return;
                }

                this.collectedParcelRepository.save(new CollectedParcel(parcel.uuid(), Instant.now()))
                    .thenCompose(saved -> this.parcelRepository.markCollected(parcel.uuid()))
                    .thenAccept(marked -> {
                        if (!Boolean.TRUE.equals(marked)) {
                            // Someone else collected it first (or the status changed under us).
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                            result.complete(null);
                            return;
                        }

                        this.parcelsByUuid.put(parcel.uuid(), withStatus(parcel, ParcelStatus.COLLECTED));
                        this.scheduler.run(() -> {
                            items.forEach(item -> ItemUtil.giveItem(player, item));
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.collected);
                        });
                        result.complete(null);
                    })
                    .exceptionally(throwable -> {
                        this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                        result.complete(null);
                        return null;
                    });
            });

            return result;
        });
    }

    private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
        return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
            parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), status);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> get(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");

        Parcel cached = this.parcelsByUuid.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.parcelRepository.findById(uuid).thenApply(optional -> {
            optional.ifPresent(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
            return optional;
        });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page) {
        Objects.requireNonNull(sender, "Sender UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");

        return this.parcelRepository.findBySender(sender, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");

        return this.parcelRepository.findByReceiver(receiver, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getCollectible(UUID receiver, UUID destinationLocker, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");

        return this.parcelRepository.findCollectible(receiver, destinationLocker, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");

        return this.parcelRepository.findReturnable(receiver, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<Boolean> markReturned(Parcel returned) {
        Objects.requireNonNull(returned, "Returned parcel cannot be null");

        return this.parcelRepository.markReturned(returned).thenApply(updated -> {
            if (updated) {
                this.parcelsByUuid.put(returned.uuid(), returned);
            }
            return updated;
        });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getAll(Page page) {
        Objects.requireNonNull(page, "Page cannot be null");
        return this.parcelRepository.findPage(page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.parcelRepository.delete(uuid).thenCompose(deleted -> {
            if (!deleted) {
                return CompletableFuture.completedFuture(false);
            }
            this.parcelsByUuid.invalidate(uuid);
            // The parcel row is gone, so reclaim its content row to avoid an orphaned leak.
            // A failed content delete only leaves an orphaned row (logged); it never affects
            // the already-deleted parcel, so the operation still reports success.
            return this.parcelContentRepository.delete(uuid)
                .exceptionally(throwable -> {
                    this.server.getLogger().warning("Failed to delete content for deleted parcel "
                        + uuid + ": " + throwable.getMessage());
                    return false;
                })
                .thenApply(contentDeleted -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.delete(parcel.uuid());
    }

    @Override
    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(noticeService, "NoticeService cannot be null");

        return this.parcelRepository.deleteAll().thenCompose(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedParcels)
                .viewer(sender)
                .placeholder(PLACEHOLDER_COUNT, deleted.toString())
                .send();

            this.parcelsByUuid.invalidateAll();

            // Reclaim every content row alongside the parcels so a bulk delete leaves nothing orphaned.
            return this.parcelContentRepository.deleteAll()
                .exceptionally(throwable -> {
                    this.server.getLogger().warning("Failed to delete parcel contents during bulk delete: "
                        + throwable.getMessage());
                    return 0;
                })
                .thenAccept(contentDeleted -> {});
        });
    }
}
