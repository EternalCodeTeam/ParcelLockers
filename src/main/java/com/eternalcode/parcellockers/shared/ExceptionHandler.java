package com.eternalcode.parcellockers.shared;

import io.sentry.Sentry;

import java.util.function.BiConsumer;

public class ExceptionHandler<T> implements BiConsumer<T, Throwable> {

    private static final ExceptionHandler<?> INSTANCE = new ExceptionHandler<>();

    private ExceptionHandler() {
    }

    @SuppressWarnings("unchecked")
    public static <T> ExceptionHandler<T> handler() {
        return (ExceptionHandler<T>) INSTANCE;
    }

    @Override
    public void accept(T result, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            Sentry.captureException(throwable);
        }
    }
}
