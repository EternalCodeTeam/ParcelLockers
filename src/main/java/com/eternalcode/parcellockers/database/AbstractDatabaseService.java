package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import io.sentry.Sentry;
import panda.std.function.ThrowingConsumer;
import panda.std.function.ThrowingFunction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractDatabaseService {

    private static final AtomicInteger EXECUTOR_COUNT = new AtomicInteger();
    protected final DataSource dataSource;
    private final ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("DATABASE-EXECUTOR-" + EXECUTOR_COUNT.incrementAndGet());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Sentry.captureException(e);
        });
        return thread;
    });

    protected AbstractDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected CompletableFuture<Void> execute(String sql, ThrowingConsumer<PreparedStatement, SQLException> consumer) {
        return this.supplyExecute(sql, statement -> {
            consumer.accept(statement);
            return null;
        });
    }

    protected <T> CompletableFuture<T> supplyExecute(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)
            ) {
                return function.apply(statement);
            }
            catch (SQLException e) {
                throw new ParcelLockersException(e);
            }
        }, this.executorService).orTimeout(5, TimeUnit.SECONDS);
    }

    protected <T> T executeSync(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function) {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            return function.apply(statement);
        }
        catch (SQLException e) {
            throw new ParcelLockersException(e);
        }
    }

}
