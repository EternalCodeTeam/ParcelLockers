package com.eternalcode.parcellockers.shared;

import com.eternalcode.parcellockers.parcel.Parcel;
import io.sentry.Sentry;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LastExceptionHandler implements BiConsumer<Optional<List<Parcel>>, Throwable> {

    private final boolean withSentryLog;

    public LastExceptionHandler() {
        this.withSentryLog = false;
    }

    public LastExceptionHandler(boolean withSentryLog) {
        this.withSentryLog = withSentryLog;
    }

    @Override
    public void accept(Optional<List<Parcel>> result, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
        if (this.withSentryLog && throwable != null) {
            Sentry.captureException(throwable);
        }
    }
}
