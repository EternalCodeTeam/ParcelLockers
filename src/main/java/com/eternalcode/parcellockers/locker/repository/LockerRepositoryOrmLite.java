package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.exception.DatabaseException;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LockerRepositoryOrmLite extends AbstractRepositoryOrmLite implements LockerRepository {

    public LockerRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), LockerTable.class);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize locker table", ex);
        }
    }

    @Override
    public CompletableFuture<Locker> save(Locker locker) {
        return this.saveIfNotExist(LockerTable.class, LockerTable.from(locker)).thenApply(LockerTable::toLocker);
    }

    @Override
    public CompletableFuture<Optional<List<Locker>>> fetchAll() {
        return this.selectAll(LockerTable.class).thenApply(lockers -> Optional.of(lockers.stream()
            .map(LockerTable::toLocker)
            .collect(Collectors.toList())));
    }

    @Override
    public CompletableFuture<Optional<Locker>> fetch(UUID uuid) {
        return this.selectSafe(LockerTable.class, uuid).thenApply(optional -> optional.map(LockerTable::toLocker));
    }

    @Override
    public CompletableFuture<Optional<Locker>> fetch(Position position) {
        return this.action(
            LockerTable.class, dao -> {
                List<LockerTable> lockers = dao.queryForEq("position", position);
                return lockers.isEmpty() ? Optional.empty() : Optional.of(lockers.getFirst().toLocker());
            });
    }

    @Override
    public CompletableFuture<Integer> delete(UUID uuid) {
        return this.deleteById(LockerTable.class, uuid);
    }

    @Override
    public CompletableFuture<Integer> delete(Locker locker) {
        return this.delete(locker.uuid());
    }

    @Override
    public CompletableFuture<PageResult<Locker>> fetchPage(Page page) {
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

            return new PageResult<>(lockers, hasNext);
        });
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(LockerTable.class);
    }
}
