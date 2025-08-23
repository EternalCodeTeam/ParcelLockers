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

    private final Cache<UUID, ItemStorage> cache = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build();

    private final ItemStorageRepository itemStorageRepository;

    public ItemStorageManager(ItemStorageRepository itemStorageRepository) {
        this.itemStorageRepository = itemStorageRepository;
    }

    public CompletableFuture<Optional<ItemStorage>> get(UUID parcelId) {
        ItemStorage content = this.cache.getIfPresent(parcelId);
        if (content != null) {
            return CompletableFuture.completedFuture(Optional.of(content));
        }
        return this.itemStorageRepository.find(parcelId).thenApply(optional -> {
            optional.ifPresent(value -> this.cache.put(parcelId, value));
            return optional;
        });
    }

    public ItemStorage getOrCreate(UUID parcelId, List<ItemStack> items) {
        return this.cache.get(parcelId, key -> this.create(key, items));
    }

    public ItemStorage create(UUID parcel, List<ItemStack> items) {
        ItemStorage content = new ItemStorage(parcel, items);
        if (this.cache.getIfPresent(parcel) != null) {
            throw new IllegalStateException("ParcelContent for parcel " + parcel + " already exists. Use ParcelContentManager#getOrCreate method instead.");
        }
        this.cache.put(parcel, content);
        this.itemStorageRepository.save(content);
        return content;
    }
    
    public void delete(UUID parcel) {
        this.cache.invalidate(parcel);
        this.itemStorageRepository.delete(parcel);
    }

    public void deleteAll(CommandSender sender, NoticeService noticeService) {
        this.itemStorageRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .viewer(sender)
                .notice(messages -> messages.admin.deletedItemStorages)
                .placeholder("{COUNT}", deleted.toString())
                .send();

            this.cache.invalidateAll();
        });
    }
}
