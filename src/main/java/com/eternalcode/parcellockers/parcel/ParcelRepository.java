package com.eternalcode.parcellockers.parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    // Updates the content and metadata of the parcel, however, it does not replace the UUID
    CompletableFuture<Void> update(Parcel oldParcel, Parcel newParcel);

    CompletableFuture<Optional<Parcel>> findByUuid(UUID uuid);

    CompletableFuture<Set<Parcel>> findAll();

    CompletableFuture<Set<Parcel>> findBySender(UUID uuid);

    CompletableFuture<Void> remove(Parcel parcel);

    CompletableFuture<Void> remove(UUID uuid);
}
