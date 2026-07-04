package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ParcelService {

    CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items);

    /**
     * Rolls back a parcel that was successfully persisted by {@link #send} but could not be
     * fully dispatched. Deletes both the parcel and its content and refunds the send fee.
     */
    CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel);

    CompletableFuture<Void> update(Parcel parcel);

    /**
     * Conditionally applies {@code updated}, refusing when the parcel's current status no
     * longer matches {@code expectedStatus} (e.g. a concurrent collect raced an admin edit).
     */
    CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus);

    CompletableFuture<Void> collect(Player player, Parcel parcel);

    CompletableFuture<Void> delete(CommandSender sender, Parcel parcel);

    CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService);

    CompletableFuture<Optional<Parcel>> get(UUID uuid);

    CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page);

    CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page);

    /**
     * Returns the delivered parcels a receiver may collect, optionally restricted to a single
     * destination locker. Filtering is applied in the query so pagination stays consistent.
     *
     * @param destinationLocker the locker to collect from, or null to allow any locker
     */
    CompletableFuture<PageResult<Parcel>> getCollectible(UUID receiver, UUID destinationLocker, Page page);

    /** Returns the COLLECTED parcels of the given receiver (candidates for a return). */
    CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page);

    /**
     * Atomically turns a COLLECTED parcel into its reverse SENT shipment. Returns false when
     * the parcel was already returned or purged in the meantime.
     */
    CompletableFuture<Boolean> markReturned(Parcel returned);

    CompletableFuture<PageResult<Parcel>> getAll(Page page);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Boolean> delete(Parcel parcel);

}
