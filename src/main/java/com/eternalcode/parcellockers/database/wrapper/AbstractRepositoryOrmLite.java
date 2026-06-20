package com.eternalcode.parcellockers.database.wrapper;

import com.eternalcode.commons.ThrowingFunction;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.DatabaseException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractRepositoryOrmLite {

    private static final Logger LOGGER = Logger.getLogger(AbstractRepositoryOrmLite.class.getName());

    protected final DatabaseManager databaseManager;
    protected final Scheduler scheduler;

    protected AbstractRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    /** Creates the backing table if it does not exist, failing fast with a {@link DatabaseException}. */
    protected void createTable(Class<?> tableType) {
        try {
            TableUtils.createTableIfNotExists(this.databaseManager.connectionSource(), tableType);
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to initialize table " + tableType.getSimpleName(), exception);
        }
    }

    /** Inserts the entity, or updates it if a row with the same id already exists. */
    protected <T> CompletableFuture<Dao.CreateOrUpdateStatus> upsert(Class<T> type, T entity) {
        return this.action(type, dao -> dao.createOrUpdate(entity));
    }

    /** Inserts the entity only if no row with the same id exists; an existing row is left untouched. */
    protected <T> CompletableFuture<T> insertIfAbsent(Class<T> type, T entity) {
        return this.action(type, dao -> dao.createIfNotExists(entity));
    }

    protected <T, ID> CompletableFuture<T> select(Class<T> type, ID id) {
        return this.action(type, dao -> dao.queryForId(id));
    }

    protected <T, ID> CompletableFuture<Optional<T>> selectSafe(Class<T> type, ID id) {
        return this.action(type, dao -> Optional.ofNullable(dao.queryForId(id)));
    }

    protected <T> CompletableFuture<Integer> delete(Class<T> type, T entity) {
        return this.action(type, dao -> dao.delete(entity));
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

    /**
     * Runs a paginated query. The {@code configure} callback may apply filters (e.g. a WHERE clause)
     * to the query builder; one extra row is fetched to determine whether a following page exists.
     */
    protected <T, ID, D> CompletableFuture<PageResult<D>> queryPage(
        Class<T> type,
        Page page,
        ThrowingFunction<QueryBuilder<T, ID>, QueryBuilder<T, ID>, SQLException> configure,
        Function<T, D> mapper
    ) {
        return this.<T, ID, PageResult<D>>action(type, dao -> {
            QueryBuilder<T, ID> builder = configure.apply(dao.queryBuilder());

            List<D> items = builder
                .limit((long) page.getLimit() + 1)
                .offset((long) page.getOffset())
                .query()
                .stream()
                .map(mapper)
                .collect(Collectors.toCollection(ArrayList::new));

            boolean hasNext = items.size() > page.getLimit();
            if (hasNext) {
                items.removeLast();
            }

            return new PageResult<>(Collections.unmodifiableList(items), hasNext);
        });
    }

    protected <T, ID, R> CompletableFuture<R> action(Class<T> type, ThrowingFunction<Dao<T, ID>, R, SQLException> action) {
        CompletableFuture<R> completableFuture = new CompletableFuture<>();

        this.scheduler.runAsync(() -> {
            Dao<T, ID> dao = this.databaseManager.getDao(type);

            try {
                completableFuture.complete(action.apply(dao));
            } catch (SQLException sqlException) {
                DatabaseException databaseException = new DatabaseException(
                    "Database operation failed for type: " + type.getSimpleName(),
                    sqlException
                );
                LOGGER.log(Level.SEVERE, "Database operation failed", databaseException);
                completableFuture.completeExceptionally(databaseException);
            } catch (Throwable throwable) {
                LOGGER.log(Level.SEVERE, "Unexpected error during database operation", throwable);
                completableFuture.completeExceptionally(throwable);
            }
        });

        return completableFuture;
    }

}
