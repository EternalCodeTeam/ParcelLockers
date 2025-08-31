package com.eternalcode.parcellockers.delivery;

import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class DeliveryManager {

    private final DeliveryRepository deliveryRepository;

    private final Cache<UUID, Delivery> deliveryCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(6))
        .maximumSize(10_000)
        .build();

    public DeliveryManager(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;

        this.cacheAll();
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

    public void delete(UUID parcel) {
        this.deliveryCache.invalidate(parcel);
        this.deliveryRepository.delete(parcel);
    }

    private void cacheAll() {
        this.deliveryRepository.fetchAll()
            .thenAccept(all -> all.ifPresent(list -> list.forEach(delivery -> this.deliveryCache.put(delivery.parcel(), delivery))));
    }
}
