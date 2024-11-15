package com.eternalcode.parcellockers.database.wrapper;

import com.eternalcode.parcellockers.database.DatabaseManager;
import com.j256.ormlite.dao.Dao;
import io.sentry.Sentry;
import panda.std.function.ThrowingFunction;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractRepositoryOrmLite {

    protected final DatabaseManager databaseManager;

    protected AbstractRepositoryOrmLite(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected <T> CompletableFuture<Dao.CreateOrUpdateStatus> save(Class<T> type, T warp) {
        return this.action(type, dao -> dao.createOrUpdate(warp));
    }

    protected <T> CompletableFuture<T> saveIfNotExist(Class<T> type, T warp) {
        return this.action(type, dao -> dao.createIfNotExists(warp));
    }

    protected <T, ID> CompletableFuture<T> select(Class<T> type, ID id) {
        return this.action(type, dao -> dao.queryForId(id));
    }

    protected <T, ID> CompletableFuture<Optional<T>> selectSafe(Class<T> type, ID id) {
        return this.action(type, dao -> Optional.ofNullable(dao.queryForId(id)));
    }

    protected <T> CompletableFuture<Integer> delete(Class<T> type, T warp) {
        return this.action(type, dao -> dao.delete(warp));
    }

    protected <T> CompletableFuture<Integer> deleteAll(Class<T> type) {
        return this.action(type, dao -> dao.deleteBuilder().delete());
    }

    protected <T, ID> CompletableFuture<Integer> deleteById(Class<T> type, ID id) {
        return this.action(type, dao -> dao.deleteById(id));
    }

    protected <T> CompletableFuture<List<T>> selectAll(Class<T> type) {
        return this.action(type, Dao::queryForAll);
    }

    protected <T, ID, R> CompletableFuture<R> action(Class<T> type, ThrowingFunction<Dao<T, ID>, R, SQLException> action) {
        CompletableFuture<R> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            Dao<T, ID> dao = this.databaseManager.getDao(type);

            try {
                completableFuture.complete(action.apply(dao));
            }
            catch (Throwable throwable) {
                Sentry.captureException(throwable);
                completableFuture.completeExceptionally(throwable);
                throwable.printStackTrace();
            }
        });

        return completableFuture;
    }

}
