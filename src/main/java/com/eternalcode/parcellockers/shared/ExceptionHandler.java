package com.eternalcode.parcellockers.shared;

import io.sentry.Sentry;

import java.util.function.BiConsumer;

// TODO replace whenCompletes with thenAccept and then use this class in whenComplete

public class ExceptionHandler<T> implements BiConsumer<T, Throwable> {

    private static final ExceptionHandler<?> INSTANCE = new ExceptionHandler<>();

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
