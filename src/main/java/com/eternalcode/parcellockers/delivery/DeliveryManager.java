package com.eternalcode.parcellockers.delivery;

import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;

public class DeliveryManager {

    private final DeliveryRepository deliveryRepository;

    private final Cache<UUID, Delivery> deliveryCache;

    public DeliveryManager(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;

        this.deliveryCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(10_000)
            .build();

        this.cacheAll();
    }

    public CompletableFuture<Optional<Delivery>> get(UUID parcel) {
        Delivery cached = this.deliveryCache.getIfPresent(parcel);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return this.deliveryRepository.find(parcel).thenApply(optional -> {
            optional.ifPresent(delivery -> this.deliveryCache.put(parcel, delivery));
            return optional;
        });
    }

    public Delivery create(UUID parcel, Instant deliveryTimestamp) {
        Delivery delivery = new Delivery(parcel, deliveryTimestamp);
        if (this.deliveryCache.getIfPresent(parcel) != null) {
            throw new IllegalStateException("Delivery for parcel " + parcel + " already exists. Use Delivery#getOrCreate method instead.");
        }
        this.deliveryCache.put(parcel, delivery);
        this.deliveryRepository.save(delivery);
        return delivery;
    }

    public CompletableFuture<Boolean> delete(UUID parcel) {
        return this.deliveryRepository.delete(parcel).thenApply(success -> {
            this.deliveryCache.invalidate(parcel);
            return success;
        });
    }

    public CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService) {
        return this.deliveryRepository.deleteAll().thenAccept(deleted -> {
            this.deliveryCache.invalidateAll();
            noticeService.create()
                .viewer(sender)
                .notice(messages -> messages.admin.deletedDeliveries)
                .placeholder("{COUNT}", deleted.toString())
                .send();
        });
    }

    private void cacheAll() {
        this.deliveryRepository.findAll()
            .thenAccept(list -> list.forEach(delivery -> this.deliveryCache.put(delivery.parcel(), delivery)));
    }
}
