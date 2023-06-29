package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel newParcel);

    CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid);

    CompletableFuture<Set<Parcel>> findBySender(UUID sender);

    CompletableFuture<Set<Parcel>> findByReceiver(UUID receiver);

    CompletableFuture<Void> remove(Parcel parcel);

    CompletableFuture<Void> remove(UUID uuid);

    CompletableFuture<ParcelPageResult> findPage(ParcelPage page);

}
