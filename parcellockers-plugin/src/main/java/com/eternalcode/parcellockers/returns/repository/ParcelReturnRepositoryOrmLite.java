package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentTable;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryTable;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelTable;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ParcelReturnRepositoryOrmLite extends AbstractRepositoryOrmLite
    implements ParcelReturnRepository {

    public ParcelReturnRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    @Override
    public CompletableFuture<Boolean> commit(
        Parcel returned,
        ParcelContent content,
        Delivery delivery
    ) {
        Objects.requireNonNull(returned, "Returned parcel cannot be null");
        Objects.requireNonNull(content, "Returned parcel content cannot be null");
        Objects.requireNonNull(delivery, "Returned parcel delivery cannot be null");

        return this.action(ParcelTable.class, parcelDao ->
            TransactionManager.callInTransaction(this.databaseManager.connectionSource(), () -> {
                UpdateBuilder<ParcelTable, Object> update = parcelDao.updateBuilder();
                update.updateColumnValue("sender", returned.sender());
                update.updateColumnValue("receiver", returned.receiver());
                update.updateColumnValue("entry_locker", returned.entryLocker());
                update.updateColumnValue("destination_locker", returned.destinationLocker());
                update.updateColumnValue("status", ParcelStatus.SENT);
                update.where()
                    .eq("uuid", returned.uuid())
                    .and()
                    .eq("status", ParcelStatus.COLLECTED);

                if (update.update() == 0) {
                    return false;
                }

                this.databaseManager.getDao(ParcelContentTable.class)
                    .createOrUpdate(ParcelContentTable.from(content));
                this.databaseManager.getDao(DeliveryTable.class)
                    .createOrUpdate(DeliveryTable.from(delivery));
                this.databaseManager.getDao(CollectedParcelTable.class)
                    .deleteById(returned.uuid());
                return true;
            }));
    }
}
