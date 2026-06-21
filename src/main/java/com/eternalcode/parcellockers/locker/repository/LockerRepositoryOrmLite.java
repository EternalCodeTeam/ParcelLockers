package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.Position;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LockerRepositoryOrmLite extends AbstractRepositoryOrmLite implements LockerRepository {

    public LockerRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.createTable(LockerTable.class);
    }

    @Override
    public CompletableFuture<Locker> save(Locker locker) {
        return this.insertIfAbsent(LockerTable.class, LockerTable.from(locker)).thenApply(LockerTable::toLocker);
    }

    @Override
    public CompletableFuture<Locker> update(Locker locker) {
        return this.upsert(LockerTable.class, LockerTable.from(locker)).thenApply(status -> locker);
    }

    @Override
    public CompletableFuture<Optional<Locker>> find(UUID uuid) {
        return this.selectSafe(LockerTable.class, uuid).thenApply(optional -> optional.map(LockerTable::toLocker));
    }

    @Override
    public CompletableFuture<Optional<Locker>> find(Position position) {
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
    public CompletableFuture<PageResult<Locker>> findPage(Page page) {
        return this.queryPage(LockerTable.class, page, builder -> builder, LockerTable::toLocker);
    }

    @Override
    public CompletableFuture<Integer> deleteAll() {
        return this.deleteAll(LockerTable.class);
    }
}
