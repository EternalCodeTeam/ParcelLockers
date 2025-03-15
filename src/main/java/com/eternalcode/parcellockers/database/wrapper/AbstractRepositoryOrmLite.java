package com.eternalcode.parcellockers.database.wrapper;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.ParcelLockers;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.j256.ormlite.dao.Dao;
import panda.std.function.ThrowingFunction;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractRepositoryOrmLite {

    protected final DatabaseManager databaseManager;
    protected final Scheduler scheduler;

    protected AbstractRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    protected <T> CompletableFuture<Dao.CreateOrUpdateStatus> save(Class<T> type, T entity) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Saving entity: {}", entity);
            return dao.createOrUpdate(entity);
        });
    }

    protected <T> CompletableFuture<T> saveIfNotExist(Class<T> type, T entity) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Saving entity (IF NOT EXIST mode): {}", entity);
            return dao.createIfNotExists(entity);
        });
    }

    protected <T, ID> CompletableFuture<T> select(Class<T> type, ID id) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Selecting: {}", id);
            return dao.queryForId(id);
        });
    }

    protected <T, ID> CompletableFuture<Optional<T>> selectSafe(Class<T> type, ID id) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Selecting (safe mode): {}", id);
            return Optional.ofNullable(dao.queryForId(id));
        });
    }

    protected <T> CompletableFuture<Integer> delete(Class<T> type, T entity) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Deleting: {}", entity);
            return dao.delete(entity);
        });
    }

    protected <T> CompletableFuture<Integer> deleteAll(Class<T> type) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Deleting all");
            return dao.deleteBuilder().delete();
        });
    }

    protected <T, ID> CompletableFuture<Integer> deleteById(Class<T> type, ID id) {
        return this.action(type, dao -> {
            ParcelLockers.DEBUG_LOGGER.info("Deleting: {}", id);
            return dao.deleteById(id);
        });
    }

    protected <T> CompletableFuture<List<T>> selectAll(Class<T> type) {
        return this.action(type, Dao::queryForAll);
    }

    protected <T, ID, R> CompletableFuture<R> action(Class<T> type, ThrowingFunction<Dao<T, ID>, R, SQLException> action) {
        CompletableFuture<R> completableFuture = new CompletableFuture<>();

        this.scheduler.runAsync(() -> {
            Dao<T, ID> dao = this.databaseManager.getDao(type);

            try {
                completableFuture.complete(action.apply(dao));
            } catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
                throwable.printStackTrace();
            }
        });

        return completableFuture;
    }

}
