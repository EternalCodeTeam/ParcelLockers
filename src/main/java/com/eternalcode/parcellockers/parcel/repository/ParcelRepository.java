package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Optional<List<Parcel>>> findAll();

    CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid);

    CompletableFuture<Optional<List<Parcel>>> findBySender(UUID sender);

    CompletableFuture<Optional<List<Parcel>>> findByReceiver(UUID receiver);

    CompletableFuture<ParcelPageResult> findByReceiver(UUID receiver, Page page);

    CompletableFuture<Integer> remove(Parcel parcel);

    CompletableFuture<Integer> remove(UUID uuid);

    CompletableFuture<Integer> removeAll();

    CompletableFuture<ParcelPageResult> findPage(Page page);
}
