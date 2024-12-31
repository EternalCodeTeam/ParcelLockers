package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
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

public class LockerRepositoryOrmLite extends AbstractRepositoryOrmLite implements LockerRepository {

    private final Map<UUID, Locker> cache = new ConcurrentHashMap<>();
    private final Map<Position, UUID> positionCache = new ConcurrentHashMap<>();

    public LockerRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), LockerWrapper.class);
        } catch (SQLException ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> save(Locker locker) {
        this.addToCache(locker);
        return this.save(LockerWrapper.class, LockerWrapper.from(locker)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<List<Locker>> findAll() {
        return this.selectAll(LockerWrapper.class).thenApply(lockers -> lockers.stream()
            .map(LockerWrapper::toLocker)
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Optional<Locker>> findByUUID(UUID uuid) {
        return this.selectSafe(LockerWrapper.class, uuid).thenApply(optional -> optional.map(LockerWrapper::toLocker));
    }

    @Override
    public CompletableFuture<Optional<Locker>> findByPosition(Position position) {
        // We have to assume that there is only one locker per position
        return this.action(LockerWrapper.class, dao -> {
            List<LockerWrapper> lockers = dao.queryForEq("position", position);
            return lockers.isEmpty() ? Optional.empty() : Optional.of(lockers.get(0).toLocker());
        });
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uuid) {
        return this.deleteById(LockerWrapper.class, uuid);
    }

    @Override
    public CompletableFuture<Integer> remove(Locker locker) {
        return this.action(LockerWrapper.class, dao -> dao.delete(LockerWrapper.from(locker)));
    }

    @Override
    public CompletableFuture<LockerPageResult> findPage(Page page) {
        return this.action(LockerWrapper.class, dao -> {
            List<Locker> lockers = dao.queryBuilder()
                .offset((long) page.getOffset())
                .limit((long) page.getLimit() + 1)
                .query()
                .stream().map(LockerWrapper::toLocker)
                .collect(Collectors.toList());

            boolean hasNext = lockers.size() > page.getLimit();
            if (hasNext) {
                lockers.remove(lockers.size() - 1);
            }

            return new LockerPageResult(lockers, hasNext);
        });
    }

    @Override
    public Optional<Locker> getFromCache(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    @Override
    public Map<UUID, Locker> cache() {
        return Collections.unmodifiableMap(this.cache);
    }

    @Override
    public Map<Position, UUID> positionCache() {
        return Collections.unmodifiableMap(this.positionCache);
    }

    @Override
    public boolean isInCache(Position position) {
        return this.positionCache.containsKey(position);
    }

    @Override
    public boolean isInCache(UUID uuid) {
        return this.cache.containsKey(uuid);
    }

    public void updateCaches() {
        this.findAll().thenAccept(lockers -> {
            this.cache.clear();
            this.positionCache.clear();
            lockers.forEach(this::addToCache);
        });
    }

    private void addToCache(Locker locker) {
        this.cache.put(locker.uuid(), locker);
        this.positionCache.put(locker.position(), locker.uuid());
    }
}
