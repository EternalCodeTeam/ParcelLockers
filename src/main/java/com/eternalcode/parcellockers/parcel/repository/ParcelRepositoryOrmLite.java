package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelRepository {

    private static final String UUID_COLUMN = "uuid";
    private static final String RECEIVER_COLUMN = "receiver";
    private static final String SENDER_COLUMN = "sender";
    private static final String DESTINATION_LOCKER_COLUMN = "destination_locker";
    private static final String ENTRY_LOCKER_COLUMN = "entry_locker";
    private static final String STATUS_COLUMN = "status";
    private static final String NAME_COLUMN = "name";
    private static final String DESCRIPTION_COLUMN = "description";
    private static final String PRIORITY_COLUMN = "priority";
    private static final String SIZE_COLUMN = "size";

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(ParcelTable.class);
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.insertIfAbsent(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> update(Parcel parcel) {
        Objects.requireNonNull(parcel, "Parcel cannot be null");
        return this.upsert(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus) {
        Objects.requireNonNull(updated, "Parcel cannot be null");
        Objects.requireNonNull(expectedStatus, "Expected status cannot be null");
        return this.action(ParcelTable.class, dao -> {
            UpdateBuilder<ParcelTable, Object> builder = dao.updateBuilder();
            builder.updateColumnValue(SENDER_COLUMN, updated.sender());
            builder.updateColumnValue(NAME_COLUMN, updated.name());
            builder.updateColumnValue(DESCRIPTION_COLUMN, updated.description());
            builder.updateColumnValue(PRIORITY_COLUMN, updated.priority());
            builder.updateColumnValue(RECEIVER_COLUMN, updated.receiver());
            builder.updateColumnValue(SIZE_COLUMN, updated.size());
            builder.updateColumnValue(ENTRY_LOCKER_COLUMN, updated.entryLocker());
            builder.updateColumnValue(DESTINATION_LOCKER_COLUMN, updated.destinationLocker());
            builder.updateColumnValue(STATUS_COLUMN, updated.status());
            builder.where()
                .eq(UUID_COLUMN, updated.uuid())
                .and()
                .eq(STATUS_COLUMN, expectedStatus);
            return builder.update() > 0;
        });
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
    public CompletableFuture<PageResult<Parcel>> findCollectible(UUID receiver, UUID destinationLocker, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");
        return this.queryPage(ParcelTable.class, page, builder -> {
            var where = builder.where()
                .eq(RECEIVER_COLUMN, receiver)
                .and()
                .eq(STATUS_COLUMN, ParcelStatus.DELIVERED);
            if (destinationLocker != null) {
                where.and().eq(DESTINATION_LOCKER_COLUMN, destinationLocker);
            }
            return builder;
        }, ParcelTable::toParcel);
    }

    @Override
    public CompletableFuture<Boolean> markCollected(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return this.action(ParcelTable.class, dao -> {
            UpdateBuilder<ParcelTable, Object> builder = dao.updateBuilder();
            builder.updateColumnValue(STATUS_COLUMN, ParcelStatus.COLLECTED);
            builder.where()
                .eq(UUID_COLUMN, uuid)
                .and()
                .eq(STATUS_COLUMN, ParcelStatus.DELIVERED);
            return builder.update() > 0;
        });
    }

    @Override
    public CompletableFuture<Boolean> markReturned(Parcel returned) {
        Objects.requireNonNull(returned, "Returned parcel cannot be null");
        return this.action(ParcelTable.class, dao -> {
            UpdateBuilder<ParcelTable, Object> builder = dao.updateBuilder();
            builder.updateColumnValue(SENDER_COLUMN, returned.sender());
            builder.updateColumnValue(RECEIVER_COLUMN, returned.receiver());
            builder.updateColumnValue(ENTRY_LOCKER_COLUMN, returned.entryLocker());
            builder.updateColumnValue(DESTINATION_LOCKER_COLUMN, returned.destinationLocker());
            builder.updateColumnValue(STATUS_COLUMN, ParcelStatus.SENT);
            builder.where()
                .eq(UUID_COLUMN, returned.uuid())
                .and()
                .eq(STATUS_COLUMN, ParcelStatus.COLLECTED);
            return builder.update() > 0;
        });
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> findReturnable(UUID receiver, Page page) {
        Objects.requireNonNull(receiver, "Receiver UUID cannot be null");
        Objects.requireNonNull(page, "Page cannot be null");
        return this.queryPage(ParcelTable.class, page, builder -> {
            builder.where()
                .eq(RECEIVER_COLUMN, receiver)
                .and()
                .eq(STATUS_COLUMN, ParcelStatus.COLLECTED);
            return builder;
        }, ParcelTable::toParcel);
    }

    @Override
    public CompletableFuture<Integer> countParcelsByDestinationLocker(UUID destinationLocker) {
        Objects.requireNonNull(destinationLocker, "Destination locker UUID cannot be null");
        return this.action(ParcelTable.class, dao -> {
            long count = dao.queryBuilder()
                .where()
                .eq(DESTINATION_LOCKER_COLUMN, destinationLocker)
                .and()
                .ne(STATUS_COLUMN, ParcelStatus.COLLECTED)
                .countOf();
            return (int) count;
        });
    }

    private CompletableFuture<PageResult<Parcel>> findByPaged(UUID key, Page page, String column) {
        return this.queryPage(ParcelTable.class, page, builder -> {
            builder.where().eq(column, key);
            return builder;
        }, ParcelTable::toParcel);
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
    public CompletableFuture<PageResult<Parcel>> findPage(Page page) {
        Objects.requireNonNull(page, "Page cannot be null");
        return this.queryPage(ParcelTable.class, page, builder -> builder, ParcelTable::toParcel);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ParcelTable.class);
    }
}
