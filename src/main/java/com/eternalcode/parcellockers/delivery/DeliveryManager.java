package com.eternalcode.parcellockers.delivery;

import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryManager {

    private final DeliveryRepository deliveryRepository;

    private final Cache<UUID, Delivery> deliveryCache;

    public DeliveryManager(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;

        this.cacheAll();
        this.deliveryCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(10_000)
            .build();
    }

    public Delivery getOrCreate(UUID parcel, Instant deliveryTimestamp) {
        return this.deliveryCache.get(parcel, key -> this.create(key, deliveryTimestamp));
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

    public CompletableFuture<Void> delete(UUID parcel) {
        return this.deliveryRepository.delete(parcel).thenApply(i -> {
            this.deliveryCache.invalidate(parcel);
            return null;
        });
    }

    private void cacheAll() {
        this.deliveryRepository.fetchAll()
            .thenAccept(all -> all.ifPresent(list -> list.forEach(delivery -> this.deliveryCache.put(delivery.parcel(), delivery))));
    }
}
