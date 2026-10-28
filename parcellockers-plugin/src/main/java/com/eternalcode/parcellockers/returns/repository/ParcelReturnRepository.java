package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.concurrent.CompletableFuture;

public interface ParcelReturnRepository {

    /**
     * Atomically claims a collected parcel for return and persists every durable part of the
     * reverse shipment. Returns false when another operation already moved the parcel out of
     * COLLECTED.
     */
    CompletableFuture<Boolean> commit(Parcel returned, ParcelContent content, Delivery delivery);
}
