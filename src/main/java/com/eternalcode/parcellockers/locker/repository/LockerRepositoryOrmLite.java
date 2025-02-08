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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LockerRepositoryOrmLite extends AbstractRepositoryOrmLite implements LockerRepository {

    private final LockerCache cache;

    public LockerRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler, LockerCache cache) {
        super(databaseManager, scheduler);
        this.cache = cache;

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), LockerWrapper.class);
        } catch (SQLException ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
            throw new RuntimeException("Failed to initialize locker table", ex);
        }
    }

    @Override
    public CompletableFuture<Void> save(Locker locker) {
        return this.save(LockerWrapper.class, LockerWrapper.from(locker)).thenApply(dao -> {
            this.cache.put(locker);
            return null;
        });
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
            return lockers.isEmpty() ? Optional.empty() : Optional.of(lockers.getFirst().toLocker());
        });
    }

    @Override
    public CompletableFuture<Integer> remove(UUID uuid) {
        return this.deleteById(LockerWrapper.class, uuid)
            .thenApply(result -> {
                if (result > 0) {
                    this.cache.remove(uuid);
                }
                return result;
            });
    }

    @Override
    public CompletableFuture<Integer> remove(Locker locker) {
        return this.remove(locker.uuid());
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
                lockers.removeLast();
            }

            return new LockerPageResult(lockers, hasNext);
        });
    }

    @Override
    public CompletableFuture<Integer> removeAll() {
        return this.deleteAll(LockerWrapper.class);
    }

    public void updateCaches() {
        this.findAll().thenAccept(lockers -> {
            Map<UUID, Locker> newCache = new ConcurrentHashMap<>();
            lockers.forEach(locker -> newCache.put(locker.uuid(), locker));
            this.cache.clear();
            this.cache.putAll(newCache);
        });
    }
}
