package com.eternalcode.parcellockers.delivery.repository;

import com.eternalcode.parcellockers.delivery.Delivery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DeliveryRepository {

    CompletableFuture<Void> save(Delivery delivery);

    CompletableFuture<Optional<Delivery>> fetch(UUID parcel);

    CompletableFuture<Integer> delete(UUID parcel);

    CompletableFuture<Integer> deleteAll();

    CompletableFuture<Optional<List<Delivery>>> fetchAll();
}
