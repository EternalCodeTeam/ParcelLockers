package com.eternalcode.parcellockers.delivery.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryRepositoryOrmLite extends AbstractRepositoryOrmLite implements DeliveryRepository {

    public DeliveryRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), DeliveryTable.class);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> save(Delivery delivery) {
        return this.saveIfNotExist(DeliveryTable.class, DeliveryTable.from(delivery)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<Delivery>> find(UUID parcel) {
        return this.selectSafe(DeliveryTable.class, parcel)
            .thenApply(optional -> optional.map(DeliveryTable::toDelivery));
    }

    @Override
    public CompletableFuture<Optional<List<Delivery>>> findAll() {
        return this.selectAll(DeliveryTable.class).thenApply(parcels -> Optional.of(parcels.stream()
            .map(DeliveryTable::toDelivery)
            .toList()));
    }

    @Override
    public CompletableFuture<Integer> delete(UUID parcel) {
        return this.deleteById(DeliveryTable.class, parcel);
    }
}
