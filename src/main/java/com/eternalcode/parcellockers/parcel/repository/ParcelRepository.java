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

    CompletableFuture<Optional<List<Parcel>>> fetchAll();

    CompletableFuture<Optional<Parcel>> fetchById(UUID uuid);

    @Deprecated
    CompletableFuture<Optional<List<Parcel>>> fetchBySender(UUID sender);

    CompletableFuture<PageResult<Parcel>> fetchBySender(UUID sender, Page page);

    @Deprecated
    CompletableFuture<Optional<List<Parcel>>> fetchByReceiver(UUID receiver);

    CompletableFuture<PageResult<Parcel>> fetchByReceiver(UUID receiver, Page page);

    CompletableFuture<Integer> delete(Parcel parcel);

    CompletableFuture<Integer> delete(UUID uuid);

    CompletableFuture<Integer> deleteAll();

    CompletableFuture<PageResult<Parcel>> fetchPage(Page page);
}
