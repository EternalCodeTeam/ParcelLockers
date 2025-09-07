package com.eternalcode.parcellockers.itemstorage;

import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class ItemStorageManager {

    private final Cache<UUID, ItemStorage> cache;

    private final ItemStorageRepository itemStorageRepository;

    public ItemStorageManager(ItemStorageRepository itemStorageRepository) {
        this.itemStorageRepository = itemStorageRepository;

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

    public ItemStorage getOrCreate(UUID owner, List<ItemStack> items) {
        return this.cache.get(owner, key -> this.create(key, items));
    }

    public ItemStorage create(UUID owner, List<ItemStack> items) {
        ItemStorage itemStorage = new ItemStorage(owner, items);
        if (this.cache.getIfPresent(owner) != null) {
            throw new IllegalStateException("ItemStorage for owner " + owner + " already exists. Use ItemStorageManager#getOrCreate method instead.");
        }
        this.cache.put(owner, itemStorage);
        this.itemStorageRepository.save(itemStorage);
        return itemStorage;
    }

    private void cacheAll() {
        this.itemStorageRepository.fetchAll()
            .thenAccept(all -> all.ifPresent(list -> list.forEach(itemStorage -> this.cache.put(
                itemStorage.owner(),
                itemStorage))));
    }
    
    public CompletableFuture<Void> delete(UUID parcel) {
        this.cache.invalidate(parcel);
        return this.itemStorageRepository.delete(parcel).thenApply(i -> null);
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
