package com.eternalcode.parcellockers.parcel;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Parcel> find(UUID parcelUUID);
}
