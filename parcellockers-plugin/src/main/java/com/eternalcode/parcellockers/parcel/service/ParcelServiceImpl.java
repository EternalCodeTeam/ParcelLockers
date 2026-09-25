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
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParcelServiceImpl implements PluginParcelService {

    private static final String PARCEL_FEE_BYPASS_PERMISSION = "parcellockers.fee.bypass";
    private static final String PLACEHOLDER_AMOUNT = "{AMOUNT}";
    private static final String PLACEHOLDER_COUNT = "{COUNT}";

    private static final long CACHE_EXPIRE_HOURS = 3;
    private static final long CACHE_MAX_SIZE = 10_000;

    private final NoticeService noticeService;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Economy economy;
    private final Server server;

    private final Cache<UUID, Parcel> parcelsByUuid;

    public ParcelServiceImpl(
        NoticeService noticeService,
        ParcelRepository parcelRepository,
        ParcelContentRepository parcelContentRepository,
        Scheduler scheduler,
        PluginConfig config,
        Economy economy,
        Server server
    ) {
        this.noticeService = noticeService;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
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
        Preconditions.checkArgument(!items.isEmpty(), "Items list cannot be empty");

        ParcelSendEvent event = new ParcelSendEvent(parcel);
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
            return CompletableFuture.completedFuture(false);
        }

        double fee = this.feeFor(sender, parcel.size());
        // Check funds before anything is persisted, so the common "cannot afford" case never leaves
        // a row behind that would need a compensating delete.
        if (fee > 0 && !this.economy.has(sender, fee)) {
            this.notifyInsufficientFunds(sender, fee);
            return CompletableFuture.completedFuture(false);
        }

        List<ItemStack> itemsCopy = items.stream()
            .map(ItemStack::clone)
            .toList();

        // Insert first, so a parcel with an already used UUID is rejected before the fee is charged
        // and can never overwrite an existing parcel.
        return this.parcelRepository.saveIfAbsent(parcel).thenCompose(inserted -> {
            if (!inserted) {
                return CompletableFuture.failedFuture(
                    new ValidationException("Parcel " + parcel.uuid() + " already exists"));
            }
            return this.chargeAndSaveContent(sender, parcel, itemsCopy, fee);
        });
    }

    private CompletableFuture<Boolean> chargeAndSaveContent(
        Player sender,
        Parcel parcel,
        List<ItemStack> items,
        double fee
    ) {
        if (fee > 0) {
            // The balance was checked before the insert, but it may have changed since.
            if (!this.economy.withdrawPlayer(sender, fee).transactionSuccess()) {
                this.notifyInsufficientFunds(sender, fee);
                return this.parcelRepository.delete(parcel.uuid())
                    .exceptionallyCompose(throwable -> CompletableFuture.failedFuture(new ParcelOperationException(
                        "Failed to remove parcel " + parcel.uuid() + " after its fee could not be charged",
                        unwrap(throwable))))
                    .thenApply(deleted -> false);
            }

            this.noticeService.create()
                .notice(messages -> messages.parcel.feeWithdrawn)
                .player(sender.getUniqueId())
                .placeholder(PLACEHOLDER_AMOUNT, formatFee(fee))
                .send();
        }

        return this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items))
            .thenApply(contentSaved -> {
                this.parcelsByUuid.put(parcel.uuid(), parcel);
                // The "sent" notice is issued by the dispatcher once the whole send succeeds, so it
                // is not shown when a later step (e.g. clearing storage) fails and rolls back.
                return true;
            })
            .exceptionallyCompose(throwable -> this.parcelRepository.delete(parcel.uuid())
                .handle((deleted, deleteError) -> {
                    // Persistence failed after the fee was withdrawn - refund it so the player is not
                    // charged for a parcel that was never created.
                    this.refundFee(sender, fee);
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
                    throw new ParcelOperationException("Failed to save parcel content, rolled back parcel", unwrap(throwable));
                }));
    }

    @Override
    public CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel) {
        this.refundFee(sender, this.feeFor(sender, parcel.size()));
        this.parcelsByUuid.invalidate(parcel.uuid());

        return this.parcelRepository.delete(parcel.uuid())
            .thenCompose(deleted -> this.parcelContentRepository.delete(parcel.uuid()))
            .thenApply(contentDeleted -> null);
    }

    private double feeFor(Player sender, ParcelSize size) {
        if (sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
            return 0;
        }
        return this.feeFor(size);
    }

    private double feeFor(ParcelSize size) {
        return switch (size) {
            case SMALL -> this.config.settings.smallParcelFee;
            case MEDIUM -> this.config.settings.mediumParcelFee;
            case LARGE -> this.config.settings.largeParcelFee;
        };
    }

    private void notifyInsufficientFunds(Player sender, double fee) {
        this.noticeService.create()
            .notice(messages -> messages.parcel.insufficientFunds)
            .player(sender.getUniqueId())
            .placeholder(PLACEHOLDER_AMOUNT, formatFee(fee))
            .send();
    }

    private static String formatFee(double fee) {
        return String.format("%.2f", fee);
    }

    private void refundFee(Player sender, double fee) {
        if (fee > 0) {
            this.economy.depositPlayer(sender, fee);
        }
    }

    @Override
    public CompletableFuture<Void> update(Parcel updated) {
        this.parcelsByUuid.put(updated.uuid(), updated);
        return this.parcelRepository.update(updated);
    }

    @Override
    public CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus) {
        return this.parcelRepository.updateIfStatus(updated, expectedStatus).thenApply(applied -> {
            if (applied) {
                this.parcelsByUuid.put(updated.uuid(), updated);
            } else {
                this.parcelsByUuid.invalidate(updated.uuid());
            }
            return applied;
        });
    }

    @Override
    public CompletableFuture<Void> delete(CommandSender sender, Parcel parcel) {

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
            // then atomically flip DELIVERED -> COLLECTED for this receiver BEFORE handing the items
            // back, so the parcel cannot be collected twice. The parcel and content rows are kept:
            // they are the snapshot a later return is validated against.
            this.scheduler.run(() -> {
                if (!canHold(player, items)) {
                    this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
                    result.complete(null);
                    return;
                }

                this.parcelRepository.commitCollection(parcel.uuid(), player.getUniqueId(), Instant.now())
                    .thenAccept(committed -> {
                        if (!committed) {
                            // Someone else collected it first, or the player is not the receiver.
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                            result.complete(null);
                            return;
                        }

                        this.parcelsByUuid.put(parcel.uuid(), withStatus(parcel, ParcelStatus.COLLECTED));
                        this.scheduler.run(() -> {
                            items.forEach(item -> ItemUtil.giveItem(player, item));
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.collected);
                            result.complete(null);
                        });
                    })
                    .exceptionally(throwable -> {
                        this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.cannotCollect);
                        result.completeExceptionally(
                            new ParcelOperationException("Failed to collect parcel " + parcel.uuid(), throwable));
                        return null;
                    });
            });

            return result;
        });
    }

    // Stages downstream of a failed future see the failure wrapped in a CompletionException.
    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
        return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
            parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), status);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> get(UUID uuid) {
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
    public void invalidate(UUID uuid) {
        this.parcelsByUuid.invalidate(uuid);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page) {

        return this.parcelRepository.findBySender(sender, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page) {

        return this.parcelRepository.findByReceiver(receiver, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getCollectible(UUID receiver, UUID destinationLocker, Page page) {

        return this.parcelRepository.findCollectible(receiver, destinationLocker, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page) {

        return this.parcelRepository.findReturnable(receiver, page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getAll(Page page) {
        return this.parcelRepository.findPage(page)
            .thenApply(result -> {
                result.items().forEach(parcel -> this.parcelsByUuid.put(parcel.uuid(), parcel));
                return result;
            });
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uuid) {
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
        return this.delete(parcel.uuid());
    }

    @Override
    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {

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
