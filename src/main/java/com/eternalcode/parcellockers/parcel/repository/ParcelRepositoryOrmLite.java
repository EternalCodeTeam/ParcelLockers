package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
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

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelTable.class);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize parcel table", ex);
        }
    }

    @Override
    public void save(Parcel parcel) {
        this.saveIfNotExist(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public void update(Parcel parcel) {
        this.save(ParcelTable.class, ParcelTable.from(parcel)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findById(UUID uuid) {
        return this.selectSafe(ParcelTable.class, uuid).thenApply(optional -> optional.map(ParcelTable::toParcel));
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> findBySender(UUID sender) {
        return this.action(
                ParcelTable.class, dao -> Optional.of(dao.queryForEq(SENDER_COLUMN, sender)
            .stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> findByReceiver(UUID receiver) {
        return this.action(
                ParcelTable.class, dao -> Optional.of(dao.queryForEq(RECEIVER_COLUMN, receiver)
            .stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    public CompletableFuture<PageResult<Parcel>> findByReceiver(UUID receiver, Page page) {
        return this.action(
                ParcelTable.class, dao -> {
            List<Parcel> parcels = dao.queryBuilder()
                .limit((long) page.getLimit() + 1)
                .offset((long) page.getOffset())
                .where()
                .eq(RECEIVER_COLUMN, receiver)
                .query()
                .stream()
                .map(ParcelTable::toParcel)
                .toList();

            boolean hasNext = parcels.size() > page.getLimit();
            if (hasNext) {
                parcels.removeLast();
            }
            return new PageResult<>(parcels, hasNext);
        });
    }

    @Override
    public CompletableFuture<Integer> remove(Parcel parcel) {
        return this.remove(parcel.uuid());
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uuid) {
        return this.deleteById(ParcelTable.class, uuid);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> findPage(Page page) {
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
    public CompletableFuture<Optional<List<Parcel>>> findAll() {
        return this.selectAll(ParcelTable.class).thenApply(parcels -> Optional.of(parcels.stream()
            .map(ParcelTable::toParcel)
            .toList()));
    }

    @Override
    public CompletableFuture<Integer> removeAll() {
        return this.deleteAll(ParcelTable.class);
    }
}
