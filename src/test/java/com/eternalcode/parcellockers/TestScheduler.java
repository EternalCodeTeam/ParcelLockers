package com.eternalcode.parcellockers;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.commons.scheduler.Task;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TestScheduler implements Scheduler {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);

    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public Task run(Runnable runnable) {
        Future<?> future = executorService.submit(runnable);
        return new TestTask(future, false);
    }

    @Override
    public Task runAsync(Runnable runnable) {
        Future<?> future = CompletableFuture.runAsync(runnable, executorService);
        return new TestTask(future, false);
    }

    @Override
    public Task runLater(Runnable runnable, Duration duration) {
        ScheduledFuture<?> future = executorService.schedule(runnable, duration.toMillis(), TimeUnit.MILLISECONDS);
        return new TestTask(future, false);
    }

    @Override
    public Task runLaterAsync(Runnable runnable, Duration duration) {
        ScheduledFuture<?> future = executorService.schedule(() -> CompletableFuture.runAsync(runnable, executorService), duration.toMillis(), TimeUnit.MILLISECONDS);
        return new TestTask(future, false);
    }

    @Override
    public Task timer(Runnable runnable, Duration initialDelay, Duration period) {
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(runnable, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return new TestTask(future, true);
    }

    @Override
    public Task timerAsync(Runnable runnable, Duration initialDelay, Duration period) {
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() -> CompletableFuture.runAsync(runnable, executorService), initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return new TestTask(future, true);
    }

    @Override
    public <T> CompletableFuture<T> complete(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }

    @Override
    public <T> CompletableFuture<T> completeAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }

    private record TestTask(Future<?> future, boolean isRepeating) implements Task {

        @Override
        public void cancel() {
            future.cancel(false);
        }

        @Override
        public boolean isCanceled() {
            return future.isCancelled();
        }

        @Override
        public boolean isAsync() {
            return future instanceof CompletableFuture || future instanceof ScheduledFuture;
        }

        @Override
        public boolean isRunning() {
            return !future.isDone();
        }
    }
}
