package com.eternalcode.parcellockers.content;

import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
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

public class ParcelContentManager {

    private final Cache<UUID, ParcelContent> cache;

    private final ParcelContentRepository contentRepository;

    public ParcelContentManager(ParcelContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();
    }

    public CompletableFuture<Optional<ParcelContent>> get(UUID parcelId) {
        ParcelContent content = this.cache.getIfPresent(parcelId);
        if (content != null) {
            return CompletableFuture.completedFuture(Optional.of(content));
        }
        return this.contentRepository.fetch(parcelId).thenApply(optional -> {
            optional.ifPresent(value -> this.cache.put(parcelId, value));
            return optional;
        });
    }

    public ParcelContent getOrCreate(UUID parcelId, List<ItemStack> items) {
        return this.cache.get(parcelId, key -> this.create(key, items));
    }

    public ParcelContent create(UUID parcel, List<ItemStack> items) {
        ParcelContent content = new ParcelContent(parcel, items);
        if (this.cache.getIfPresent(parcel) != null) {
            throw new IllegalStateException("ParcelContent for parcel " + parcel + " already exists. Use ParcelContentManager#getOrCreate method instead.");
        }
        this.cache.put(parcel, content);
        this.contentRepository.save(content);
        return content;
    }

    public CompletableFuture<Boolean> delete(UUID parcel) {
        return this.contentRepository.delete(parcel).thenApply(i -> {
            this.cache.invalidate(parcel);
            return i > 0;
        });
    }

    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.contentRepository.deleteAll().thenAccept(deleted -> {
            noticeService.create()
                .viewer(sender)
                .notice(messages -> messages.admin.deletedContents)
                .placeholder("{COUNT}", deleted.toString())
                .send();
            this.cache.invalidateAll();
        });
    }
}
