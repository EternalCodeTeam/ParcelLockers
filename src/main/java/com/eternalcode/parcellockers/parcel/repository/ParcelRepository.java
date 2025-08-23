package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Optional<List<Parcel>>> findAll();

    CompletableFuture<Optional<Parcel>> findById(UUID uuid);

    CompletableFuture<Optional<List<Parcel>>> findBySender(UUID sender);

    CompletableFuture<Optional<List<Parcel>>> findByReceiver(UUID receiver);

    CompletableFuture<PageResult<Parcel>> findByReceiver(UUID receiver, Page page);

    CompletableFuture<Integer> delete(Parcel parcel);

    CompletableFuture<Integer> delete(UUID uuid);

    CompletableFuture<Integer> deleteAll();

    CompletableFuture<PageResult<Parcel>> findPage(Page page);
}
