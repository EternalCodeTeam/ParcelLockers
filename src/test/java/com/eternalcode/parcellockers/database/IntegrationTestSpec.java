package com.eternalcode.parcellockers.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class IntegrationTestSpec {

    <T> T await(CompletableFuture<T> future) {
        return future
            .orTimeout(5, TimeUnit.SECONDS)
            .join();
    }
}
