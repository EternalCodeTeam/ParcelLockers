package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid);

    CompletableFuture<Optional<List<Parcel>>> findBySender(UUID sender);

    CompletableFuture<Optional<List<Parcel>>> findByReceiver(UUID receiver);

    CompletableFuture<Void> remove(Parcel parcel);

    CompletableFuture<Void> remove(UUID uuid);

    CompletableFuture<ParcelPageResult> findPage(Page page);

    Map<UUID, Parcel> cache();

    Optional<Parcel> findParcel(UUID uuid);

}
