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
     * Finds the parcels a receiver is allowed to collect: those addressed to them whose status is
     * {@link com.eternalcode.parcellockers.parcel.ParcelStatus#DELIVERED}. Filtering happens in the
     * query so pagination operates on the eligible set rather than on a raw receiver page.
     *
     * @param receiver          the receiver whose parcels are collected
     * @param destinationLocker when non-null, only parcels addressed to this locker are returned;
     *                          when null, delivered parcels from any locker are returned
     * @param page              the requested page
     */
    CompletableFuture<PageResult<Parcel>> findCollectible(UUID receiver, UUID destinationLocker, Page page);

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
