package com.eternalcode.parcellockers.itemstorage;

import com.eternalcode.parcellockers.itemstorage.event.ItemStorageUpdateEvent;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class ItemStorageManager {

    private final Cache<UUID, ItemStorage> cache;

    private final ItemStorageRepository itemStorageRepository;
    private final Server server;

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
        ItemStorage oldItemStorage = this.cache.getIfPresent(owner);
        ItemStorage newItemStorage = new ItemStorage(owner, items);

        // This is an update operation - fire ItemStorageUpdateEvent
        ItemStorageUpdateEvent event = new ItemStorageUpdateEvent(oldItemStorage, newItemStorage);
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("ItemStorage update was cancelled by event"));
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
        return this.itemStorageRepository.delete(owner).thenApply(i -> {
            this.cache.invalidate(owner);
            return i > 0;
        });
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
}
