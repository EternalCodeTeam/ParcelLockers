package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Plugin-internal extension of the public {@link ParcelService}, exposing operations that only the
 * plugin itself (GUIs, commands, dispatch workflow) is allowed to perform.
 */
public interface PluginParcelService extends ParcelService {

    /**
     * Undoes a successful {@link #send(Player, Parcel, java.util.List)}: refunds the sender's fee
     * (unless they bypass it) and deletes the parcel together with its content.
     *
     * @param sender the player who sent the parcel and should be refunded
     * @param parcel the parcel to remove
     * @return a future completed once the parcel and its content are deleted
     */
    CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel);

    /**
     * Evicts a parcel from the in-memory cache, forcing the next read to hit the database.
     *
     * @param uuid the parcel to evict
     */
    void invalidate(UUID uuid);

    /**
     * Deletes a parcel on behalf of a command sender and notifies them about the outcome.
     *
     * @param sender the viewer who receives the success or failure notice
     * @param parcel the parcel to delete
     * @return a future completed once the deletion finished; failures are reported to the sender
     */
    CompletableFuture<Void> delete(CommandSender sender, Parcel parcel);

    /**
     * Deletes every parcel and its content, then notifies the sender how many parcels were removed.
     *
     * @param sender        the viewer who receives the notice
     * @param noticeService the notice service used to send the summary
     * @return a future completed once all parcels and contents are deleted
     */
    CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService);
}
