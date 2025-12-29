package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.DatabaseException;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelRepository {

    private static final String RECEIVER_COLUMN = "receiver";
    private static final String SENDER_COLUMN = "sender";
    private static final String DESTINATION_LOCKER_COLUMN = "destination_locker";
    private static final String STATUS_COLUMN = "status";

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelTable.class);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize parcel table", ex);
        }
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.saveIfNotExist(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> update(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.save(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findById(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.selectSafe(ParcelTable.class, uuid)
            .thenApply(optional -> optional.map(ParcelTable::toParcel));
    }

    @Override
    public CompletableFuture<List<Parcel>> findBySender(UUID sender) {
        Objects.requireNonNull(sender, "Sender UUID cannot be null");
        return this.action(
            ParcelTable.class,
            dao -> dao.queryForEq(SENDER_COLUMN, sender).stream()
                .map(ParcelTable::toParcel)
                .toList()
        );
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> findBySender(UUID sender, Page page) {
        Objects.requireNonNull(sender, "Sender UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");
        return this.findByPaged(sender, page, SENDER_COLUMN);
    }

    @Override
    public CompletableFuture<List<Parcel>> findByReceiver(UUID receiver) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        return this.action(
            ParcelTable.class,
            dao -> dao.queryForEq(RECEIVER_COLUMN, receiver).stream()
                .map(ParcelTable::toParcel)
                .toList()
        );
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> findByReceiver(UUID receiver, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");
        return this.findByPaged(receiver, page, RECEIVER_COLUMN);
    }

    @Override
    public CompletableFuture<Integer> countDeliveredParcelsByDestinationLocker(UUID destinationLocker) {
        Objects.requireNonNull(destinationLocker, "Destination locker UUID cannot be null");
        return this.action(ParcelTable.class, dao -> {
            long count = dao.queryBuilder()
                .where()
                .eq(DESTINATION_LOCKER_COLUMN, destinationLocker)
                .and()
                .eq(STATUS_COLUMN, ParcelStatus.DELIVERED)
                .countOf();
            return (int) count;
        });
    }

    private CompletableFuture<PageResult<Parcel>> findByPaged(UUID key, Page page, String column) {
        return this.action(
            ParcelTable.class, dao -> {
                List<Parcel> parcels = dao.queryBuilder()
                    .limit((long) page.getLimit() + 1)
                    .offset((long) page.getOffset())
                    .where()
                    .eq(column, key)
                    .query()
                    .stream()
                    .map(ParcelTable::toParcel)
                    .collect(Collectors.toCollection(ArrayList::new));

                boolean hasNext = parcels.size() > page.getLimit();
                if (hasNext) {
                    parcels.removeLast();
                }

                return new PageResult<>(Collections.unmodifiableList(parcels), hasNext);
            });
    }

    @Override
    public CompletableFuture<Boolean> delete(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.delete(parcel.uuid());
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.deleteById(ParcelTable.class, uuid).thenApply(rows -> rows > 0);
    }

    @Override
    public CompletableFuture<List<Parcel>> findAll() {
        return this.selectAll(ParcelTable.class)
            .thenApply(parcels -> parcels.stream()
                .map(ParcelTable::toParcel)
                .toList());
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ParcelTable.class);
    }
}
