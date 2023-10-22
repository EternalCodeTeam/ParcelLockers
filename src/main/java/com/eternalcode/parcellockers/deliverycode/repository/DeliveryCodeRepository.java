package com.eternalcode.parcellockers.deliverycode.repository;

import com.eternalcode.parcellockers.deliverycode.DeliveryCode;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DeliveryCodeRepository {

    CompletableFuture<Void> save(DeliveryCode deliveryCode);

    CompletableFuture<Optional<DeliveryCode>> findByUUID(UUID parcelUUID);

    CompletableFuture<Void> remove(DeliveryCode deliveryCode);

    CompletableFuture<Void> remove(UUID parcelUUID);

}
