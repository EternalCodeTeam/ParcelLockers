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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LockerRepositoryOrmLite extends AbstractRepositoryOrmLite implements LockerRepository {

    private final LockerCache cache;

    public LockerRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler, LockerCache cache) {
        super(databaseManager, scheduler);
        this.cache = cache;

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), LockerTable.class);
        } catch (SQLException ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
            throw new RuntimeException("Failed to initialize locker table", ex);
        }
    }

    @Override
    public CompletableFuture<Void> save(Locker locker) {
        return this.save(LockerTable.class, LockerTable.from(locker)).thenApply(dao -> {
            this.cache.put(locker);
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<List<Locker>>> findAll() {
        return this.selectAll(LockerTable.class).thenApply(lockers -> Optional.of(lockers.stream()
            .map(LockerTable::toLocker)
            .collect(Collectors.toList())));
    }

    @Override
    public CompletableFuture<Optional<Locker>> find(UUID uuid) {
        return this.selectSafe(LockerTable.class, uuid).thenApply(optional -> optional.map(LockerTable::toLocker));
    }

    @Override
    public CompletableFuture<Optional<Locker>> find(Position position) {
        // We have to assume that there is only one locker per position
        return this.action(
                LockerTable.class, dao -> {
            List<LockerTable> lockers = dao.queryForEq("position", position);
            return lockers.isEmpty() ? Optional.empty() : Optional.of(lockers.getFirst().toLocker());
        });
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
        return this.deleteById(LockerTable.class, uuid)
            .thenApply(result -> {
                if (result > 0) {
                    this.cache.remove(uuid);
                }
                return result;
            });
    }

    @Override
    public CompletableFuture<Integer> delete(Locker locker) {
        return this.delete(locker.uuid());
    }

    @Override
    public CompletableFuture<LockerPageResult> findPage(Page page) {
        return this.action(
                LockerTable.class, dao -> {
            List<Locker> lockers = dao.queryBuilder()
                .offset((long) page.getOffset())
                .limit((long) page.getLimit() + 1)
                .query()
                .stream().map(LockerTable::toLocker)
                .collect(Collectors.toList());

            boolean hasNext = lockers.size() > page.getLimit();
            if (hasNext) {
                lockers.removeLast();
            }

            return new LockerPageResult(lockers, hasNext);
        });
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(LockerTable.class);
    }

    public void updateCaches() {
        this.findAll().thenAccept(lockers -> {
            Map<UUID, Locker> newCache = new HashMap<>();
            if (lockers.isPresent()) {
                lockers.get().forEach(locker -> newCache.put(locker.uuid(), locker));
                this.cache.clear();
                this.cache.putAll(newCache);
            }
        });
    }
}
