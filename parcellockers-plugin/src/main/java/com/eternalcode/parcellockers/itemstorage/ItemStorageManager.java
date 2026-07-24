package com.eternalcode.parcellockers.itemstorage;

import com.eternalcode.parcellockers.itemstorage.event.ItemStorageUpdateEvent;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class ItemStorageManager {

    private final Cache<UUID, ItemStorage> cache;

    private final ItemStorageRepository itemStorageRepository;
    private final Server server;
    private final Object reservationLock = new Object();
    private final Map<UUID, Reservation> reservations = new HashMap<>();
    private final Map<UUID, Integer> ordinaryOperations = new HashMap<>();
    private boolean globalOperation;

    public ItemStorageManager(ItemStorageRepository itemStorageRepository, Server server) {
        this.itemStorageRepository = itemStorageRepository;
        this.server = server;

        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

        this.cacheAll();
    }

    public CompletableFuture<Optional<ItemStorage>> get(UUID parcelId) {
        return this.runOrdinaryOperation(parcelId, () -> {
            ItemStorage content = this.cache.getIfPresent(parcelId);
            if (content != null) {
                return CompletableFuture.completedFuture(Optional.of(content));
            }
            return this.itemStorageRepository.fetch(parcelId).thenApply(optional -> {
                optional.ifPresent(value -> this.cache.put(parcelId, value));
                return optional;
            });
        });
    }

    public CompletableFuture<ItemStorage> getOrCreate(UUID owner, List<ItemStack> items) {
        return this.runOrdinaryOperation(owner, () -> {
            ItemStorage existing = this.cache.getIfPresent(owner);
            if (existing != null) {
                return CompletableFuture.completedFuture(existing);
            }
            return this.itemStorageRepository.fetch(owner).thenCompose(persisted -> {
                if (persisted.isPresent()) {
                    ItemStorage itemStorage = persisted.get();
                    this.cache.put(owner, itemStorage);
                    return CompletableFuture.completedFuture(itemStorage);
                }
                return this.createInternal(owner, items, true);
            });
        });
    }

    public CompletableFuture<ItemStorage> create(UUID owner, List<ItemStack> items) {
        return this.runOrdinaryOperation(owner, () -> this.createInternal(owner, items, true));
    }

    private CompletableFuture<ItemStorage> createInternal(
        UUID owner,
        List<ItemStack> items,
        boolean fireEvent
    ) {
        ItemStorage oldItemStorage = this.cache.getIfPresent(owner);
        ItemStorage newItemStorage = new ItemStorage(owner, items);

        if (fireEvent) {
            // This is an update operation - fire ItemStorageUpdateEvent
            ItemStorageUpdateEvent event = new ItemStorageUpdateEvent(oldItemStorage, newItemStorage);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("ItemStorage update was cancelled by event"));
            }
        }

        this.cache.put(owner, newItemStorage);
        // Return the save future so callers can react to a persistence failure instead of losing items.
        // If the save fails, undo the optimistic cache update so the cache never holds an unpersisted
        // storage (which, combined with re-giving the items to the player, would duplicate them).
        return this.itemStorageRepository.save(newItemStorage)
            .whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    if (oldItemStorage != null) {
                        this.cache.put(owner, oldItemStorage);
                    } else {
                        this.cache.invalidate(owner);
                    }
                }
            })
            .thenApply(ignored -> newItemStorage);
    }

    private void cacheAll() {
        this.runGlobalOperation(() -> this.itemStorageRepository.fetchAll()
            .thenAccept(all -> all.ifPresent(list -> list.forEach(itemStorage -> this.cache.put(
                itemStorage.owner(),
                itemStorage)))));
    }
    
    public CompletableFuture<Boolean> delete(UUID owner) {
        return this.runOrdinaryOperation(owner, () -> this.deleteInternal(owner));
    }

    public Optional<ItemStorageReservation> reserve(UUID owner) {
        synchronized (this.reservationLock) {
            if (this.globalOperation
                || this.reservations.containsKey(owner)
                || this.ordinaryOperations.getOrDefault(owner, 0) > 0) {
                return Optional.empty();
            }
            Reservation reservation = new Reservation(owner);
            this.reservations.put(owner, reservation);
            return Optional.of(reservation);
        }
    }

    private CompletableFuture<Boolean> deleteReserved(Reservation reservation) {
        return this.runReservedOperation(
            reservation,
            () -> this.deleteInternal(reservation.owner()));
    }

    private CompletableFuture<ItemStorage> restoreReserved(
        Reservation reservation,
        List<ItemStack> items
    ) {
        return this.runReservedOperation(
            reservation,
            () -> this.createInternal(reservation.owner(), items, false));
    }

    private CompletableFuture<Boolean> deleteInternal(UUID owner) {
        return this.itemStorageRepository.delete(owner).thenApply(i -> {
            this.cache.invalidate(owner);
            return i > 0;
        });
    }

    private <T> CompletableFuture<T> runOrdinaryOperation(
        UUID owner,
        Supplier<CompletableFuture<T>> action
    ) {
        if (!this.beginOrdinaryOperation(owner)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage is unavailable for ordinary operation: "
                    + owner));
        }

        try {
            return action.get()
                .whenComplete((ignored, throwable) -> this.endOrdinaryOperation(owner));
        } catch (Throwable throwable) {
            this.endOrdinaryOperation(owner);
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private boolean beginOrdinaryOperation(UUID owner) {
        synchronized (this.reservationLock) {
            if (this.globalOperation || this.reservations.containsKey(owner)) {
                return false;
            }
            this.ordinaryOperations.merge(owner, 1, Integer::sum);
            return true;
        }
    }

    private void endOrdinaryOperation(UUID owner) {
        synchronized (this.reservationLock) {
            int remaining = this.ordinaryOperations.getOrDefault(owner, 0) - 1;
            if (remaining > 0) {
                this.ordinaryOperations.put(owner, remaining);
            } else {
                this.ordinaryOperations.remove(owner);
            }
        }
    }

    private <T> CompletableFuture<T> runReservedOperation(
        Reservation reservation,
        Supplier<CompletableFuture<T>> action
    ) {
        if (!this.beginReservedOperation(reservation)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage reservation is no longer active: "
                    + reservation.owner()));
        }

        try {
            return action.get().whenComplete(
                (ignored, throwable) -> this.endReservedOperation(reservation));
        } catch (Throwable throwable) {
            this.endReservedOperation(reservation);
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private boolean beginReservedOperation(Reservation reservation) {
        synchronized (this.reservationLock) {
            if (this.reservations.get(reservation.owner()) != reservation
                || reservation.closed.get()) {
                return false;
            }
            reservation.activeOperations++;
            return true;
        }
    }

    private void endReservedOperation(Reservation reservation) {
        synchronized (this.reservationLock) {
            reservation.activeOperations--;
            if (reservation.activeOperations == 0 && reservation.closed.get()) {
                this.reservations.remove(reservation.owner(), reservation);
            }
        }
    }

    private void requestRelease(Reservation reservation) {
        synchronized (this.reservationLock) {
            if (reservation.activeOperations == 0) {
                this.reservations.remove(reservation.owner(), reservation);
            }
        }
    }

    private <T> CompletableFuture<T> runGlobalOperation(
        Supplier<CompletableFuture<T>> action
    ) {
        if (!this.beginGlobalOperation()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage has active owner operations"));
        }

        try {
            return action.get()
                .whenComplete((ignored, throwable) -> this.endGlobalOperation());
        } catch (Throwable throwable) {
            this.endGlobalOperation();
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private boolean beginGlobalOperation() {
        synchronized (this.reservationLock) {
            if (this.globalOperation
                || !this.reservations.isEmpty()
                || !this.ordinaryOperations.isEmpty()) {
                return false;
            }
            this.globalOperation = true;
            return true;
        }
    }

    private void endGlobalOperation() {
        synchronized (this.reservationLock) {
            this.globalOperation = false;
        }
    }

    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.runGlobalOperation(() -> this.itemStorageRepository.deleteAll()
            .thenAccept(deleted -> {
            this.cache.invalidateAll();

            noticeService.create()
                .viewer(sender)
                .notice(messages -> messages.admin.deletedItemStorages)
                .placeholder("{COUNT}", deleted.toString())
                .send();
        }));
    }

    private final class Reservation implements ItemStorageReservation {

        private final UUID owner;
        private final AtomicBoolean closed = new AtomicBoolean();
        private int activeOperations;

        private Reservation(UUID owner) {
            this.owner = owner;
        }

        @Override
        public UUID owner() {
            return this.owner;
        }

        @Override
        public CompletableFuture<Boolean> delete() {
            return ItemStorageManager.this.deleteReserved(this);
        }

        @Override
        public CompletableFuture<ItemStorage> restore(List<ItemStack> items) {
            return ItemStorageManager.this.restoreReserved(this, items);
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) {
                ItemStorageManager.this.requestRelease(this);
            }
        }
    }
}
