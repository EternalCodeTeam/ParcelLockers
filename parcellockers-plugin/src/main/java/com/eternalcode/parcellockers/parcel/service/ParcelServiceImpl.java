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
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
    private static final Executor DEFAULT_OPERATION_HANDOFF = command -> {
        try {
            Thread.ofVirtual()
                .name("parcel-operation-handoff")
                .start(command);
        } catch (Throwable virtualThreadFailure) {
            Thread fallback = new Thread(command, "parcel-operation-handoff-fallback");
            fallback.setDaemon(true);
            fallback.start();
        }
    };

    private final NoticeService noticeService;
    private final ParcelRepository parcelRepository;
    private final ParcelContentRepository parcelContentRepository;
    private final CollectedParcelRepository collectedParcelRepository;
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Economy economy;
    private final Server server;
    private final Executor operationHandoff;

    private final Cache<UUID, Parcel> parcelsByUuid;
    private final Map<UUID, CompletableFuture<Void>> parcelOperationTails = new HashMap<>();
    private CompletableFuture<Void> globalParcelOperationTail =
        CompletableFuture.completedFuture(null);
    private final ThreadLocal<UUID> parcelOperationCallback = new ThreadLocal<>();

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
        this(
            noticeService, parcelRepository, parcelContentRepository,
            collectedParcelRepository, scheduler, config, economy, server,
            DEFAULT_OPERATION_HANDOFF);
    }

    ParcelServiceImpl(
        NoticeService noticeService,
        ParcelRepository parcelRepository,
        ParcelContentRepository parcelContentRepository,
        CollectedParcelRepository collectedParcelRepository,
        Scheduler scheduler,
        PluginConfig config,
        Economy economy,
        Server server,
        Executor operationHandoff
    ) {
        this.noticeService = noticeService;
        this.parcelRepository = parcelRepository;
        this.parcelContentRepository = parcelContentRepository;
        this.collectedParcelRepository = collectedParcelRepository;
        this.scheduler = scheduler;
        this.config = config;
        this.economy = economy;
        this.server = server;
        this.operationHandoff = Objects.requireNonNull(
            operationHandoff, "Operation handoff cannot be null");

        this.parcelsByUuid = Caffeine.newBuilder()
            .expireAfterAccess(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
            .maximumSize(CACHE_MAX_SIZE)
            .build();
    }

    @Override
    public CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.serializeParcelOperation(
            parcel.uuid(), () -> this.sendWithinParcelOperation(sender, parcel, items));
    }

    @Override
    public CompletableFuture<Boolean> sendWithinParcelOperation(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    ) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        Objects.requireNonNull(items, "Items list cannot be null");
        Preconditions.checkArgument(!items.isEmpty(), "Items list cannot be empty");

        ParcelSendEvent event = new ParcelSendEvent(parcel);
        this.runParcelOperationCallback(
            parcel.uuid(), () -> this.server.getPluginManager().callEvent(event));

        if (event.isCancelled()) {
            this.noticeService.player(sender.getUniqueId(), messages -> messages.parcel.cannotSend);
            return CompletableFuture.completedFuture(false);
        }

        List<ItemStack> itemsCopy = items.stream()
            .map(ItemStack::clone)
            .toList();

        try {
            return this.parcelRepository.saveIfAbsent(parcel)
                .thenCompose(inserted -> {
                    if (!Boolean.TRUE.equals(inserted)) {
                        return CompletableFuture.failedFuture(new ValidationException(
                            "Parcel " + parcel.uuid() + " already exists"));
                    }
                    return this.persistReservedParcel(sender, parcel, itemsCopy);
                })
                .exceptionallyCompose(throwable -> {
                    Throwable cause = unwrap(throwable);
                    if (cause instanceof ValidationException validationException) {
                        return CompletableFuture.failedFuture(validationException);
                    }
                    if (cause instanceof ParcelOperationException operationException) {
                        return CompletableFuture.failedFuture(operationException);
                    }
                    return CompletableFuture.failedFuture(new ParcelOperationException(
                        "Failed to reserve parcel " + parcel.uuid(), cause));
                });
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(new ParcelOperationException(
                "Failed to reserve parcel " + parcel.uuid(), unwrap(throwable)));
        }
    }

    private CompletableFuture<Boolean> persistReservedParcel(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    ) {
        double chargedFee = 0;
        try {
            if (!sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
                double fee = this.feeFor(parcel.size());
                if (fee > 0) {
                    boolean success = this.economy.withdrawPlayer(sender, fee).transactionSuccess();
                    String formattedFee = String.format("%.2f", fee);
                    if (!success) {
                        this.notifyInsufficientFunds(sender, formattedFee);
                        return this.removeReservation(parcel).thenApply(ignored -> false);
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
            return this.parcelContentRepository.save(new ParcelContent(parcel.uuid(), items))
                .thenApply(contentSaved -> {
                    this.parcelsByUuid.put(parcel.uuid(), parcel);
                    return true;
                })
                .exceptionallyCompose(throwable -> this.compensatePersistenceFailure(
                    sender, parcel, refundableFee, unwrap(throwable)));
        } catch (Throwable throwable) {
            return this.compensatePersistenceFailure(
                sender, parcel, chargedFee, unwrap(throwable));
        }
    }

    private CompletableFuture<Void> removeReservation(Parcel parcel) {
        return this.parcelRepository.delete(parcel.uuid()).thenCompose(deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new ParcelOperationException(
                "Failed to remove unpaid parcel reservation " + parcel.uuid(),
                new IllegalStateException("Parcel reservation delete returned false")));
        });
    }

    private void notifyInsufficientFunds(Player sender, String formattedFee) {
        try {
            this.noticeService.create()
                .notice(messages -> messages.parcel.insufficientFunds)
                .player(sender.getUniqueId())
                .placeholder(PLACEHOLDER_AMOUNT, formattedFee)
                .send();
        } catch (Throwable ignored) {
            // A presentation failure must not change the business outcome.
        }
    }

    private CompletableFuture<Boolean> compensatePersistenceFailure(
        Player sender,
        Parcel parcel,
        double refundableFee,
        Throwable trigger
    ) {
        ParcelOperationException failure = new ParcelOperationException(
            "Failed to persist parcel " + parcel.uuid(),
            trigger
        );
        List<Throwable> cleanupFailures = new ArrayList<>();
        this.parcelsByUuid.invalidate(parcel.uuid());

        return this.attemptRollback(
                () -> this.parcelRepository.delete(parcel.uuid()),
                "Delete parcel " + parcel.uuid(),
                cleanupFailures
            )
            .thenCompose(ignored -> this.attemptRollback(
                () -> this.parcelContentRepository.delete(parcel.uuid()),
                "Delete parcel content " + parcel.uuid(),
                cleanupFailures
            ))
            .thenCompose(ignored -> this.attemptRollback(() -> {
                this.refundFee(sender, refundableFee);
                return CompletableFuture.completedFuture(null);
            }, "Refund parcel fee for " + parcel.uuid(), cleanupFailures))
            .thenCompose(ignored -> {
                cleanupFailures.forEach(failure::addSuppressed);
                this.notifyCannotSend(sender);
                return CompletableFuture.failedFuture(failure);
            });
    }

    @Override
    public CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.serializeParcelOperation(
            parcel.uuid(), () -> this.rollbackSendWithinParcelOperation(sender, parcel));
    }

    @Override
    public CompletableFuture<Void> rollbackSendWithinParcelOperation(
        Player sender,
        Parcel parcel
    ) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");

        List<Throwable> failures = new ArrayList<>();
        try {
            if (!sender.hasPermission(PARCEL_FEE_BYPASS_PERMISSION)) {
                this.refundFee(sender, this.feeFor(parcel.size()));
            }
        } catch (Throwable throwable) {
            failures.add(unwrap(throwable));
        }
        this.parcelsByUuid.invalidate(parcel.uuid());

        return this.attemptRollback(
                () -> this.parcelRepository.delete(parcel.uuid()),
                "Delete parcel " + parcel.uuid(),
                failures
            )
            .thenCompose(ignored -> this.attemptRollback(
                () -> this.parcelContentRepository.delete(parcel.uuid()),
                "Delete parcel content " + parcel.uuid(),
                failures
            ))
            .thenCompose(ignored -> {
                if (failures.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                ParcelOperationException failure = new ParcelOperationException(
                    "Failed to completely roll back parcel " + parcel.uuid(),
                    failures.get(0)
                );
                failures.stream().skip(1).forEach(failure::addSuppressed);
                return CompletableFuture.failedFuture(failure);
            });
    }

    private CompletableFuture<Void> attemptRollback(
        java.util.function.Supplier<CompletableFuture<?>> action,
        String description,
        List<Throwable> failures
    ) {
        CompletableFuture<?> result;
        try {
            result = action.get();
        } catch (Throwable throwable) {
            failures.add(unwrap(throwable));
            return CompletableFuture.completedFuture(null);
        }
        if (result == null) {
            failures.add(new IllegalStateException("Rollback action returned a null future"));
            return CompletableFuture.completedFuture(null);
        }
        return result.handle((value, throwable) -> {
            if (throwable != null) {
                failures.add(unwrap(throwable));
            } else if (value instanceof Boolean completed && !completed) {
                failures.add(new IllegalStateException(
                    description + " returned false"));
            }
            return null;
        });
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
            if (!this.economy.depositPlayer(sender, fee).transactionSuccess()) {
                throw new IllegalStateException(
                    "Economy provider rejected parcel fee refund");
            }
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.CompletionException
            && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private void notifyCannotSend(Player sender) {
        try {
            this.noticeService.player(
                sender.getUniqueId(), messages -> messages.parcel.cannotSend);
        } catch (Throwable throwable) {
            if (this.server.getLogger() != null) {
                this.server.getLogger().warning(
                    "Failed to send parcel failure notice to " + sender.getUniqueId()
                        + ": " + throwable.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<Void> update(Parcel updated) {
        Objects.requireNonNull(updated, "Updated parcel cannot be null");
        return this.serializeParcelOperation(
            updated.uuid(), () -> this.updateWithinParcelOperation(updated));
    }

    @Override
    public CompletableFuture<Void> updateWithinParcelOperation(Parcel updated) {
        this.parcelsByUuid.put(updated.uuid(), updated);
        return this.parcelRepository.update(updated);
    }

    @Override
    public CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus) {
        Objects.requireNonNull(updated, "Updated parcel cannot be null");
        Objects.requireNonNull(expectedStatus, "Expected status cannot be null");
        return this.serializeParcelOperation(
            updated.uuid(),
            () -> this.updateIfStatusWithinParcelOperation(updated, expectedStatus));
    }

    @Override
    public CompletableFuture<Boolean> updateIfStatusWithinParcelOperation(
        Parcel updated,
        ParcelStatus expectedStatus
    ) {
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
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.serializeParcelOperation(
            parcel.uuid(), () -> this.deleteWithinOperation(sender, parcel));
    }

    private CompletableFuture<Void> deleteWithinOperation(CommandSender sender, Parcel parcel) {
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
        return this.serializeParcelOperation(
            parcel.uuid(), () -> this.collectWithinOperation(player, parcel));
    }

    private CompletableFuture<Void> collectWithinOperation(Player player, Parcel parcel) {
        UUID playerId = player.getUniqueId();
        CompletableFuture<Optional<Parcel>> authoritativeFuture;
        try {
            authoritativeFuture = this.parcelRepository.findById(parcel.uuid());
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(new ParcelOperationException(
                "Failed to load parcel " + parcel.uuid(), unwrap(throwable)));
        }
        return authoritativeFuture.thenCompose(optional -> {
            Parcel authoritative = optional.orElseThrow(() ->
                new ValidationException("Parcel " + parcel.uuid() + " does not exist"));
            if (!playerId.equals(authoritative.receiver())) {
                throw new ValidationException("Player is not the parcel receiver");
            }
            if (authoritative.status() != ParcelStatus.DELIVERED) {
                throw new ValidationException("Parcel must have DELIVERED status");
            }
            return this.collectAuthoritative(player, authoritative);
        }).exceptionallyCompose(throwable -> {
            Throwable cause = unwrap(throwable);
            if (cause instanceof ValidationException validationException) {
                return CompletableFuture.failedFuture(validationException);
            }
            if (cause instanceof ParcelOperationException operationException) {
                return CompletableFuture.failedFuture(operationException);
            }
            return CompletableFuture.failedFuture(new ParcelOperationException(
                "Failed to collect parcel " + parcel.uuid(), cause));
        });
    }

    private CompletableFuture<Void> collectAuthoritative(Player player, Parcel parcel) {
        UUID playerId = player.getUniqueId();
        return this.fireCollectEvent(parcel).thenCompose(cancelled -> {
            if (cancelled) {
                this.notifyCannotCollect(playerId);
                return CompletableFuture.completedFuture(null);
            }

            return this.parcelContentRepository.find(parcel.uuid()).thenCompose(optional -> {
                if (optional.isEmpty()) {
                    this.notifyCannotCollect(playerId);
                    return CompletableFuture.completedFuture(null);
                }

                List<ItemStack> items = optional.get().items();
                CompletableFuture<Void> result = new CompletableFuture<>();
                this.scheduleCollection(player, parcel, items, result);
                return result;
            });
        });
    }

    private void scheduleCollection(
        Player player,
        Parcel parcel,
        List<ItemStack> items,
        CompletableFuture<Void> result
    ) {
        UUID playerId = player.getUniqueId();
        try {
            this.scheduler.run(() -> {
                try {
                    if (!canHold(player, items)) {
                        this.notifyNoInventorySpace(playerId);
                        result.complete(null);
                        return;
                    }

                    Instant collectedAt = Instant.now();
                    this.parcelRepository.commitCollection(
                            parcel.uuid(), playerId, collectedAt)
                        .whenComplete((marked, throwable) -> {
                            if (throwable != null) {
                                this.notifyCannotCollect(playerId);
                                result.completeExceptionally(new ParcelOperationException(
                                    "Failed to persist collection for parcel " + parcel.uuid(),
                                    unwrap(throwable)));
                                return;
                            }
                            if (!Boolean.TRUE.equals(marked)) {
                                this.notifyCannotCollect(playerId);
                                result.complete(null);
                                return;
                            }

                            this.parcelsByUuid.put(
                                parcel.uuid(), withStatus(parcel, ParcelStatus.COLLECTED));
                            this.scheduleCollectedItems(
                                player, parcel, items, collectedAt, result);
                        });
                } catch (Throwable throwable) {
                    result.completeExceptionally(new ParcelOperationException(
                        "Failed to collect parcel " + parcel.uuid(), unwrap(throwable)));
                }
            });
        } catch (Throwable throwable) {
            result.completeExceptionally(new ParcelOperationException(
                "Failed to schedule collection for parcel " + parcel.uuid(), unwrap(throwable)));
        }
    }

    private void scheduleCollectedItems(
        Player player,
        Parcel parcel,
        List<ItemStack> items,
        Instant collectedAt,
        CompletableFuture<Void> result
    ) {
        try {
            this.scheduler.run(() -> {
                ItemStack[] inventorySnapshot = null;
                try {
                    inventorySnapshot = Arrays.stream(player.getInventory().getContents())
                        .map(item -> item == null ? null : item.clone())
                        .toArray(ItemStack[]::new);
                    items.forEach(item -> ItemUtil.giveItem(player, item));
                    this.notifyCollected(player.getUniqueId());
                    result.complete(null);
                } catch (Throwable throwable) {
                    this.failCollectionDelivery(
                        player, parcel, collectedAt, inventorySnapshot,
                        unwrap(throwable), result);
                }
            });
        } catch (Throwable throwable) {
            this.failCollectionDelivery(
                player, parcel, collectedAt, null, unwrap(throwable), result);
        }
    }

    private void failCollectionDelivery(
        Player player,
        Parcel parcel,
        Instant collectedAt,
        ItemStack[] inventorySnapshot,
        Throwable trigger,
        CompletableFuture<Void> result
    ) {
        ParcelOperationException failure = new ParcelOperationException(
            "Failed to give collected parcel items " + parcel.uuid(), trigger);
        if (inventorySnapshot != null) {
            try {
                player.getInventory().setContents(inventorySnapshot);
            } catch (Throwable restoreFailure) {
                failure.addSuppressed(unwrap(restoreFailure));
                this.parcelsByUuid.invalidate(parcel.uuid());
                result.completeExceptionally(failure);
                return;
            }
        }

        CompletableFuture<Boolean> rollback;
        try {
            rollback = this.parcelRepository.rollbackCollection(
                parcel.uuid(), player.getUniqueId(), collectedAt);
        } catch (Throwable rollbackFailure) {
            failure.addSuppressed(unwrap(rollbackFailure));
            this.parcelsByUuid.invalidate(parcel.uuid());
            result.completeExceptionally(failure);
            return;
        }
        rollback.whenComplete((rolledBack, rollbackFailure) -> {
            if (rollbackFailure != null) {
                failure.addSuppressed(unwrap(rollbackFailure));
            } else if (!Boolean.TRUE.equals(rolledBack)) {
                failure.addSuppressed(new IllegalStateException(
                    "Collection rollback returned false"));
            }
            this.parcelsByUuid.invalidate(parcel.uuid());
            result.completeExceptionally(failure);
        });
    }

    private void notifyCannotCollect(UUID player) {
        try {
            this.noticeService.player(player, messages -> messages.parcel.cannotCollect);
        } catch (Throwable ignored) {
            // Notices are best-effort and must never leave an operation future incomplete.
        }
    }

    private void notifyNoInventorySpace(UUID player) {
        try {
            this.noticeService.player(player, messages -> messages.parcel.noInventorySpace);
        } catch (Throwable ignored) {
            // Notices are best-effort and must never leave an operation future incomplete.
        }
    }

    private void notifyCollected(UUID player) {
        try {
            this.noticeService.player(player, messages -> messages.parcel.collected);
        } catch (Throwable ignored) {
            // Collection is already committed; notification failure is non-transactional.
        }
    }

    private CompletableFuture<Boolean> fireCollectEvent(Parcel parcel) {
        if (this.server.isPrimaryThread()) {
            try {
                ParcelCollectEvent event = new ParcelCollectEvent(parcel);
                this.runParcelOperationCallback(
                    parcel.uuid(), () -> this.server.getPluginManager().callEvent(event));
                return CompletableFuture.completedFuture(event.isCancelled());
            } catch (Throwable throwable) {
                return CompletableFuture.failedFuture(throwable);
            }
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            this.scheduler.run(() -> {
                try {
                    ParcelCollectEvent event = new ParcelCollectEvent(parcel);
                    this.runParcelOperationCallback(
                        parcel.uuid(), () -> this.server.getPluginManager().callEvent(event));
                    result.complete(event.isCancelled());
                } catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            });
        } catch (Throwable throwable) {
            result.completeExceptionally(throwable);
        }
        return result;
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
    public void invalidate(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        this.parcelsByUuid.invalidate(uuid);
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
        return this.serializeParcelOperation(uuid, () -> this.deleteWithinOperation(uuid));
    }

    @Override
    public CompletableFuture<Optional<Parcel>> getAuthoritativeWithinParcelOperation(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.parcelRepository.findById(uuid).thenApply(optional -> {
            if (optional.isPresent()) {
                this.parcelsByUuid.put(uuid, optional.get());
            } else {
                this.parcelsByUuid.invalidate(uuid);
            }
            return optional;
        });
    }

    private CompletableFuture<Boolean> deleteWithinOperation(UUID uuid) {
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
    public <T> CompletableFuture<T> serializeParcelOperation(
        UUID parcel,
        Supplier<CompletableFuture<T>> operation
    ) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        Objects.requireNonNull(operation, "Parcel operation cannot be null");
        if (this.parcelOperationCallback.get() != null) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Parcel mutations cannot be started from a parcel event callback"));
        }

        CompletableFuture<Void> gate = new CompletableFuture<>();
        CompletableFuture<Void> predecessor;
        CompletableFuture<Void> globalPredecessor;
        synchronized (this.parcelOperationTails) {
            predecessor = this.parcelOperationTails.put(parcel, gate);
            globalPredecessor = this.globalParcelOperationTail;
        }
        if (predecessor == null) {
            predecessor = CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> ready = CompletableFuture.allOf(
            predecessor.handle((ignored, throwable) -> null),
            globalPredecessor.handle((ignored, throwable) -> null));
        CompletableFuture<T> result = new CompletableFuture<>();
        ready.thenRun(() -> {
            CompletableFuture<T> running;
            try {
                running = Objects.requireNonNull(
                    operation.get(), "Parcel operation returned a null future");
            } catch (Throwable throwable) {
                completeSerializedOperation(gate, result, null, throwable);
                return;
            }
            running.whenComplete((value, throwable) ->
                completeSerializedOperation(gate, result, value, throwable));
        });
        gate.whenComplete((ignored, throwable) -> {
            synchronized (this.parcelOperationTails) {
                this.parcelOperationTails.remove(parcel, gate);
            }
        });
        return result;
    }

    @Override
    public void runParcelOperationCallback(UUID parcel, Runnable callback) {
        Objects.requireNonNull(parcel, "Parcel UUID cannot be null");
        Objects.requireNonNull(callback, "Parcel callback cannot be null");
        UUID previous = this.parcelOperationCallback.get();
        this.parcelOperationCallback.set(parcel);
        try {
            callback.run();
        } finally {
            if (previous == null) {
                this.parcelOperationCallback.remove();
            } else {
                this.parcelOperationCallback.set(previous);
            }
        }
    }

    @Override
    public boolean isParcelOperationCallbackActive() {
        return this.parcelOperationCallback.get() != null;
    }

    private <T> CompletableFuture<T> serializeAllParcelOperations(
        Supplier<CompletableFuture<T>> operation
    ) {
        if (this.parcelOperationCallback.get() != null) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Bulk parcel mutations cannot be started from a parcel event callback"));
        }
        CompletableFuture<Void> gate = new CompletableFuture<>();
        CompletableFuture<Void> ready;
        synchronized (this.parcelOperationTails) {
            List<CompletableFuture<Void>> predecessors =
                new ArrayList<>(this.parcelOperationTails.values());
            predecessors.add(this.globalParcelOperationTail);
            ready = CompletableFuture.allOf(predecessors.toArray(CompletableFuture[]::new));
            this.globalParcelOperationTail = gate;
        }

        CompletableFuture<T> result = new CompletableFuture<>();
        ready.handle((ignored, throwable) -> null).thenRun(() -> {
            CompletableFuture<T> running;
            try {
                running = Objects.requireNonNull(
                    operation.get(), "Global parcel operation returned a null future");
            } catch (Throwable throwable) {
                completeSerializedOperation(gate, result, null, throwable);
                return;
            }
            running.whenComplete((value, throwable) ->
                completeSerializedOperation(gate, result, value, throwable));
        });
        gate.whenComplete((ignored, throwable) -> {
            synchronized (this.parcelOperationTails) {
                if (this.globalParcelOperationTail == gate) {
                    this.globalParcelOperationTail = CompletableFuture.completedFuture(null);
                }
            }
        });
        return result;
    }

    private <T> void completeSerializedOperation(
        CompletableFuture<Void> gate,
        CompletableFuture<T> result,
        T value,
        Throwable throwable
    ) {
        Runnable releaseGate = () -> gate.complete(null);
        try {
            this.operationHandoff.execute(releaseGate);
        } catch (Throwable handoffFailure) {
            try {
                CompletableFuture.runAsync(releaseGate);
            } catch (Throwable emergencyHandoffFailure) {
                handoffFailure.addSuppressed(emergencyHandoffFailure);
                releaseGate.run();
            }
        }
        if (throwable != null) {
            result.completeExceptionally(throwable);
        } else {
            result.complete(value);
        }
    }

    @Override
    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(noticeService, "NoticeService cannot be null");
        return this.serializeAllParcelOperations(
            () -> this.deleteAllWithinOperation(sender, noticeService));
    }

    private CompletableFuture<Void> deleteAllWithinOperation(
        CommandSender sender,
        NoticeService noticeService
    ) {
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
