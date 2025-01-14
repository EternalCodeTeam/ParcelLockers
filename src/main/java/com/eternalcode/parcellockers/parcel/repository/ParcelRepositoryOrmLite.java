package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;
import com.j256.ormlite.table.TableUtils;
import io.sentry.Sentry;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ParcelRepositoryOrmLite extends AbstractRepositoryOrmLite implements ParcelRepository {

    private static final String RECEIVER_COLUMN = "receiver";
    private static final String SENDER_COLUMN = "sender";

    private final ParcelCache cache;

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler, ParcelCache cache) {
        super(databaseManager, scheduler);
        this.cache = cache;

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), ParcelWrapper.class);
        } catch (SQLException ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        return this.save(ParcelWrapper.class, ParcelWrapper.from(parcel)).thenApply(dao -> {
            this.cache.put(parcel);
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid) {
        return this.selectSafe(ParcelWrapper.class, uuid).thenApply(optional -> optional.map(ParcelWrapper::toParcel));
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> findBySender(UUID sender) {
        return this.action(ParcelWrapper.class, dao -> Optional.of(dao.queryForEq(SENDER_COLUMN, sender)
            .stream()
            .map(ParcelWrapper::toParcel)
            .toList()));
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> findByReceiver(UUID receiver) {
        return this.action(ParcelWrapper.class, dao -> Optional.of(dao.queryForEq(RECEIVER_COLUMN, receiver)
            .stream()
            .map(ParcelWrapper::toParcel)
            .toList()));
    }

    @Override
    public CompletableFuture<Integer> remove(Parcel parcel) {
        return this.remove(parcel.uuid());
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uuid) {
        CompletableFuture<Integer> removeFuture = this.deleteById(ParcelWrapper.class, uuid);
        removeFuture.thenAccept(deletedCount -> {
            if (deletedCount > 0) {
                this.cache.remove(uuid);
            }
        });
        return removeFuture;
    }

    @Override
    public CompletableFuture<ParcelPageResult> findPage(Page page) {
        return this.action(ParcelWrapper.class, dao -> {
            List<com.eternalcode.parcellockers.parcel.Parcel> parcels = dao.queryBuilder()
                .limit((long) page.getLimit() + 1)
                .offset((long) page.getOffset())
                .query()
                .stream().map(ParcelWrapper::toParcel)
                .collect(Collectors.toList());

            boolean hasNext = parcels.size() > page.getLimit();
            if (hasNext) {
                parcels.removeLast();
            }
            return new ParcelPageResult(parcels, hasNext);
        });
    }

    @Override
    public CompletableFuture<Optional<List<Parcel>>> findAll() {
        return this.selectAll(ParcelWrapper.class).thenApply(parcels -> Optional.of(parcels.stream()
            .map(ParcelWrapper::toParcel)
            .toList()));
    }

    public void updateCaches() {
        this.findAll().thenAccept(parcels -> {
            List<Parcel> parcelList = parcels.orElse(List.of());
            Map<UUID, Parcel> newCache = new ConcurrentHashMap<>();

            parcelList.forEach(parcel -> newCache.put(parcel.uuid(), parcel));
            this.cache.clear();
            this.cache.putAll(newCache);
        });
    }
}
