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
import com.eternalcode.parcellockers.parcel.event.ParcelCollectEvent;
import com.eternalcode.parcellockers.parcel.event.ParcelSendEvent;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
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
                    this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.sent);
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
            // then delete the parcel BEFORE handing the items back so it cannot be collected twice.
            // Items are only given once the delete is confirmed, so a failed delete never destroys them.
            this.scheduler.run(() -> {
                if (!canHold(player, items)) {
                    this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.noInventorySpace);
                    result.complete(null);
                    return;
                }

                this.parcelRepository.delete(parcel)
                    .thenCompose(deleted -> this.parcelContentRepository.delete(parcel.uuid())
                        .thenApply(contentDeleted -> deleted && contentDeleted))
                    .thenAccept(removed -> {
                        if (!removed) {
                            this.noticeService.player(player.getUniqueId(), messages -> messages.parcel.databaseError);
                            result.complete(null);
                            return;
                        }

                        this.parcelsByUuid.invalidate(parcel.uuid());
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
    public CompletableFuture<Boolean> delete(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.parcelRepository.delete(uuid).thenApply(deleted -> {
            if (deleted) {
                Parcel cached = this.parcelsByUuid.getIfPresent(uuid);
                if (cached != null) {
                    this.parcelsByUuid.invalidate(cached.uuid());
                } else {
                    this.parcelsByUuid.invalidate(uuid);
                }
            }
            return deleted;
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

        return this.parcelRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .notice(messages -> messages.admin.deletedParcels)
                .viewer(sender)
                .placeholder(PLACEHOLDER_COUNT, deleted.toString())
                .send();

            this.parcelsByUuid.invalidateAll();
        });
    }
}
