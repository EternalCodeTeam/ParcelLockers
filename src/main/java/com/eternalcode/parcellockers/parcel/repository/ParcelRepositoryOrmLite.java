package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;
import com.j256.ormlite.table.TableUtils;
import io.sentry.Sentry;

import java.sql.SQLException;
import java.util.Collections;
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

    private final Map<UUID, Parcel> cache = new ConcurrentHashMap<>();

    public ParcelRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

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
            this.addToCache(parcel);
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
                this.removeFromCache(uuid);
            }
        });
        return removeFuture;
    }

    @Override
    public CompletableFuture<ParcelPageResult> findPage(Page page) {
        return this.action(ParcelWrapper.class, dao -> {
            List<Parcel> parcels = dao.queryBuilder()
                .limit((long) page.getLimit() + 1)
                .offset((long) page.getOffset())
                .query()
                .stream().map(ParcelWrapper::toParcel)
                .collect(Collectors.toList());

            boolean hasNext = parcels.size() > page.getLimit();
            if (hasNext) {
                parcels.remove(parcels.size() - 1);
            }
            return new ParcelPageResult(parcels, hasNext);
        });
    }

    @Override
    public Map<UUID, Parcel> cache() {
        return Collections.unmodifiableMap(this.cache);
    }

    private void addToCache(Parcel parcel) {
        this.cache.put(parcel.uuid(), parcel);
    }

    private void removeFromCache(UUID uuid) {
        this.cache.remove(uuid);
    }

    @Override
    public Optional<Parcel> findParcel(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }
}
