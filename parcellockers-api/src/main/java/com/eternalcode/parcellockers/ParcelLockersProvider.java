package com.eternalcode.parcellockers;

import java.util.Objects;

public final class ParcelLockersProvider {

    private static volatile ParcelLockersApi api;

    private ParcelLockersProvider() {
    }

    public static ParcelLockersApi provide() {
        ParcelLockersApi current = api;
        if (current == null) {
            throw new IllegalStateException("ParcelLockersApi has not been initialized");
        }
        return current;
    }

    static synchronized void initialize(ParcelLockersApi parcelLockersApi) {
        Objects.requireNonNull(parcelLockersApi, "parcelLockersApi");
        if (api != null) {
            throw new IllegalStateException("ParcelLockersApi has already been initialized");
        }
        api = parcelLockersApi;
    }

    static synchronized void deinitialize() {
        if (api == null) {
            throw new IllegalStateException("ParcelLockersApi has not been initialized");
        }
        api = null;
    }
}
