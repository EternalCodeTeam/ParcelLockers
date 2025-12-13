package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelRepository {

    private static final String RECEIVER_COLUMN = "receiver";
    private static final String SENDER_COLUMN = "sender";
    private static final String DESTINATION_LOCKER_COLUMN = "destination_locker";

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelTable.class);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize parcel table", ex);
        }
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        return this.saveIfNotExist(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> update(Parcel parcel) {
        return this.save(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> fetchById(UUID uuid) {
        return this.selectSafe(ParcelTable.class, uuid).thenApply(optional -> optional.map(ParcelTable::toParcel));
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> fetchBySender(UUID sender) {
        return this.action(
                ParcelTable.class, dao -> Optional.of(dao.queryForEq(SENDER_COLUMN, sender)
            .stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    public CompletableFuture<PageResult<Parcel>> fetchBySender(UUID sender, Page page) {
        return this.fetchByPaged(sender, page, SENDER_COLUMN);
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> fetchByReceiver(UUID receiver) {
        return this.action(
                ParcelTable.class, dao -> Optional.of(dao.queryForEq(RECEIVER_COLUMN, receiver)
            .stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    public CompletableFuture<PageResult<Parcel>> fetchByReceiver(UUID receiver, Page page) {
        return this.fetchByPaged(receiver, page, RECEIVER_COLUMN);
    }

    @Override
    public CompletableFuture<Integer> countByDestinationLocker(UUID destinationLocker) {
        return this.action(ParcelTable.class, dao -> {
            long count = dao.queryBuilder()
                .where()
                .eq(DESTINATION_LOCKER_COLUMN, destinationLocker)
                .and()
                .eq("status", ParcelStatus.DELIVERED)
                .countOf();
            return (int) count;
        });
    }

    private CompletableFuture<PageResult<Parcel>> fetchByPaged(UUID key, Page page, String column) {
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
                    .collect(Collectors.toList());

                boolean hasNext = parcels.size() > page.getLimit();
                if (hasNext) {
                    parcels.removeLast();
                }
                return new PageResult<>(parcels, hasNext);
            });
    }

    @Override
    public CompletableFuture<Integer> delete(Parcel parcel) {
        return this.delete(parcel.uuid());
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
        return this.deleteById(ParcelTable.class, uuid);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> fetchPage(Page page) {
        return this.action(
                ParcelTable.class, dao -> {
            List<Parcel> parcels = dao.queryBuilder()
                .limit((long) page.getLimit() + 1)
                .offset((long) page.getOffset())
                .query()
                .stream().map(ParcelTable::toParcel)
                .collect(Collectors.toList());

            boolean hasNext = parcels.size() > page.getLimit();
            if (hasNext) {
                parcels.removeLast();
            }
            return new PageResult<>(parcels, hasNext);
        });
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> fetchAll() {
        return this.selectAll(ParcelTable.class).thenApply(parcels -> Optional.of(parcels.stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(ParcelTable.class);
    }
}
