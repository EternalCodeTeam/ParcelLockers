package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcellocker.ParcelLocker;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel oldParcel, Parcel newParcel);

    CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid);

    CompletableFuture<Set<Parcel>> findBySender(UUID sender);

    CompletableFuture<Set<Parcel>> findByReceiver(UUID receiver);

    CompletableFuture<Set<Parcel>> findAll();

    CompletableFuture<Void> remove(Parcel parcel);

    CompletableFuture<Void> remove(UUID uuid);

    CompletableFuture<List<Parcel>> findPage(int page, int pageSize);
}
