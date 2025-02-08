package com.eternalcode.parcellockers.shared;

import io.sentry.Sentry;

import java.util.function.BiConsumer;

public class SentryExceptionHandler<T> implements BiConsumer<T, Throwable> {

    private static final SentryExceptionHandler<?> INSTANCE = new SentryExceptionHandler<>();

    private SentryExceptionHandler() {
    }

    @SuppressWarnings("unchecked")
    public static <T> SentryExceptionHandler<T> handler() {
        return (SentryExceptionHandler<T>) INSTANCE;
    }

    @Override
    public void accept(T result, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            Sentry.captureException(throwable);
        }
    }
}
