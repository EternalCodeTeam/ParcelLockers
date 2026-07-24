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
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class ItemStorageManager {

    private final Cache<UUID, ItemStorage> cache;

    private final ItemStorageRepository itemStorageRepository;
    private final Server server;
    private final Object reservationLock = new Object();
    private final Map<UUID, Reservation> reservations = new HashMap<>();
    private final Map<UUID, Integer> ordinaryMutations = new HashMap<>();

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
        ItemStorage content = this.cache.getIfPresent(parcelId);
        if (content != null) {
            return CompletableFuture.completedFuture(Optional.of(content));
        }
        return this.itemStorageRepository.fetch(parcelId).thenApply(optional -> {
            optional.ifPresent(value -> this.cache.put(parcelId, value));
            return optional;
        });
    }

    public CompletableFuture<ItemStorage> getOrCreate(UUID owner, List<ItemStack> items) {
        ItemStorage existing = this.cache.getIfPresent(owner);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        // Do not call create() from inside cache.get(owner, loader): create() writes the same key
        // back into the cache, and Caffeine forbids mutating the key being computed.
        return this.create(owner, items);
    }

    public CompletableFuture<ItemStorage> create(UUID owner, List<ItemStack> items) {
        if (!this.beginOrdinaryMutation(owner)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage is reserved for active dispatch: " + owner));
        }

        CompletableFuture<ItemStorage> operation;
        try {
            operation = this.createInternal(owner, items, true);
        } catch (Throwable throwable) {
            this.endOrdinaryMutation(owner);
            return CompletableFuture.failedFuture(throwable);
        }
        return operation.whenComplete((ignored, throwable) -> this.endOrdinaryMutation(owner));
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
        this.itemStorageRepository.fetchAll()
            .thenAccept(all -> all.ifPresent(list -> list.forEach(itemStorage -> this.cache.put(
                itemStorage.owner(),
                itemStorage))));
    }
    
    public CompletableFuture<Boolean> delete(UUID owner) {
        if (!this.beginOrdinaryMutation(owner)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage is reserved for active dispatch: " + owner));
        }

        CompletableFuture<Boolean> operation;
        try {
            operation = this.deleteInternal(owner);
        } catch (Throwable throwable) {
            this.endOrdinaryMutation(owner);
            return CompletableFuture.failedFuture(throwable);
        }
        return operation.whenComplete((ignored, throwable) -> this.endOrdinaryMutation(owner));
    }

    public Optional<ItemStorageReservation> reserve(UUID owner) {
        synchronized (this.reservationLock) {
            if (this.reservations.containsKey(owner)
                || this.ordinaryMutations.getOrDefault(owner, 0) > 0) {
                return Optional.empty();
            }
            Reservation reservation = new Reservation(owner);
            this.reservations.put(owner, reservation);
            return Optional.of(reservation);
        }
    }

    private CompletableFuture<Boolean> deleteReserved(Reservation reservation) {
        if (!this.isActive(reservation)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage reservation is no longer active: "
                    + reservation.owner()));
        }
        return this.deleteInternal(reservation.owner());
    }

    private CompletableFuture<ItemStorage> restoreReserved(
        Reservation reservation,
        List<ItemStack> items
    ) {
        if (!this.isActive(reservation)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Item storage reservation is no longer active: "
                    + reservation.owner()));
        }
        return this.createInternal(reservation.owner(), items, false);
    }

    private CompletableFuture<Boolean> deleteInternal(UUID owner) {
        return this.itemStorageRepository.delete(owner).thenApply(i -> {
            this.cache.invalidate(owner);
            return i > 0;
        });
    }

    private boolean beginOrdinaryMutation(UUID owner) {
        synchronized (this.reservationLock) {
            if (this.reservations.containsKey(owner)) {
                return false;
            }
            this.ordinaryMutations.merge(owner, 1, Integer::sum);
            return true;
        }
    }

    private void endOrdinaryMutation(UUID owner) {
        synchronized (this.reservationLock) {
            int remaining = this.ordinaryMutations.getOrDefault(owner, 0) - 1;
            if (remaining > 0) {
                this.ordinaryMutations.put(owner, remaining);
            } else {
                this.ordinaryMutations.remove(owner);
            }
        }
    }

    private boolean isActive(Reservation reservation) {
        synchronized (this.reservationLock) {
            return this.reservations.get(reservation.owner()) == reservation
                && !reservation.closed.get();
        }
    }

    private void release(Reservation reservation) {
        synchronized (this.reservationLock) {
            this.reservations.remove(reservation.owner(), reservation);
        }
    }

    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.itemStorageRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .viewer(sender)
                .notice(messages -> messages.admin.deletedItemStorages)
                .placeholder("{COUNT}", deleted.toString())
                .send();

            this.cache.invalidateAll();
        });
    }

    private final class Reservation implements ItemStorageReservation {

        private final UUID owner;
        private final AtomicBoolean closed = new AtomicBoolean();

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
                ItemStorageManager.this.release(this);
            }
        }
    }
}
