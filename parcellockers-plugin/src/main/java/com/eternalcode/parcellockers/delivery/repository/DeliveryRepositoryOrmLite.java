package com.eternalcode.parcellockers.delivery.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.delivery.Delivery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryRepositoryOrmLite extends AbstractRepositoryOrmLite implements DeliveryRepository {

    public DeliveryRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(DeliveryTable.class);
    }

    @Override
    public CompletableFuture<Void> save(Delivery delivery) {
        return this.insertIfAbsent(DeliveryTable.class, DeliveryTable.from(delivery)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> update(Delivery delivery) {
        return this.upsert(DeliveryTable.class, DeliveryTable.from(delivery)).thenApply(status -> null);
    }

    @Override
    public CompletableFuture<Optional<Delivery>> find(UUID parcel) {
        return this.selectSafe(DeliveryTable.class, parcel)
            .thenApply(optional -> optional.map(DeliveryTable::toDelivery));
    }

    @Override
    public CompletableFuture<List<Delivery>> findAll() {
        return this.selectAll(DeliveryTable.class).thenApply(parcels -> parcels.stream()
            .map(DeliveryTable::toDelivery)
            .toList());
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID parcel) {
        return this.deleteById(DeliveryTable.class, parcel).thenApply(i -> i > 0);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(DeliveryTable.class);
    }
}
