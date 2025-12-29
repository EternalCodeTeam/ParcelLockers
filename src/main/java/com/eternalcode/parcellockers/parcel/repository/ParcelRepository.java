package com.eternalcode.parcellockers.parcel.repository;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.TestOnly;

public interface ParcelRepository {

    CompletableFuture<Void> save(Parcel parcel);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<List<Parcel>> findAll();

    CompletableFuture<Optional<Parcel>> findById(UUID uuid);

    @TestOnly
    CompletableFuture<List<Parcel>> findBySender(UUID sender);

    CompletableFuture<PageResult<Parcel>> findBySender(UUID sender, Page page);

    @TestOnly
    CompletableFuture<List<Parcel>> findByReceiver(UUID receiver);

    CompletableFuture<PageResult<Parcel>> findByReceiver(UUID receiver, Page page);

    CompletableFuture<Integer> countDeliveredParcelsByDestinationLocker(UUID destinationLocker);

    CompletableFuture<Boolean> delete(Parcel parcel);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Integer> deleteAll();
}
