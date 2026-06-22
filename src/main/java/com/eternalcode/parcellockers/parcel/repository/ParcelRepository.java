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

    CompletableFuture<PageResult<Parcel>> findPage(Page page);

    CompletableFuture<Optional<Parcel>> findById(UUID uuid);

    @TestOnly
    CompletableFuture<List<Parcel>> findBySender(UUID sender);

    CompletableFuture<PageResult<Parcel>> findBySender(UUID sender, Page page);

    @TestOnly
    CompletableFuture<List<Parcel>> findByReceiver(UUID receiver);

    CompletableFuture<PageResult<Parcel>> findByReceiver(UUID receiver, Page page);

    /**
     * Counts the parcels currently occupying a destination locker. Collected parcels are removed
     * from storage, so every parcel addressed to the locker (in-transit or delivered) occupies a
     * slot. Counting in-transit parcels reserves a slot at send time and closes the fullness race.
     */
    CompletableFuture<Integer> countParcelsByDestinationLocker(UUID destinationLocker);

    CompletableFuture<Boolean> delete(Parcel parcel);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Integer> deleteAll();
}
