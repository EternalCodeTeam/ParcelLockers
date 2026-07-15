package com.eternalcode.parcellockers.returns.task;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deletes collected parcels whose return window expired: the parcel row, its content row and the
 * collected_parcels row. Runs periodically; failures are logged and retried on the next run.
 */
public class ReturnWindowPurgeTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ReturnWindowPurgeTask.class.getName());

    /** Extra slack past the window so an in-flight return at the expiry boundary cannot race the purge. */
    private static final Duration GRACE = Duration.ofMinutes(5);

    private final ParcelService parcelService;
    private final CollectedParcelRepository collectedParcelRepository;
    private final DeliveryManager deliveryManager;
    private final PluginConfig config;

    public ReturnWindowPurgeTask(
        ParcelService parcelService,
        CollectedParcelRepository collectedParcelRepository,
        DeliveryManager deliveryManager,
        PluginConfig config
    ) {
        this.parcelService = parcelService;
        this.collectedParcelRepository = collectedParcelRepository;
        this.deliveryManager = deliveryManager;
        this.config = config;
    }

    @Override
    public void run() {
        Instant cutoff = Instant.now().minus(this.config.settings.parcelReturnWindow).minus(GRACE);

        this.collectedParcelRepository.findExpired(cutoff)
            .thenAccept(expired -> expired.forEach(this::purge))
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to query expired collected parcels", throwable);
                return null;
            });
    }

    private void purge(CollectedParcel collected) {
        this.parcelService.get(collected.parcel())
            .thenCompose(optionalParcel -> {
                if (optionalParcel.isPresent() && optionalParcel.get().status() == ParcelStatus.COLLECTED) {
                    // Delete the parcel (and content) first; the row is only removed once that
                    // succeeded so a failed delete is retried on the next run.
                    return this.parcelService.delete(collected.parcel())
                        .thenCompose(deleted -> Boolean.TRUE.equals(deleted)
                            ? this.collectedParcelRepository.delete(collected.parcel())
                            : CompletableFuture.completedFuture(false))
                        .thenCompose(unused -> {
                            // Best-effort: clears a stray delivery row left behind by a failed
                            // delete at ParcelSendTask.deliver time; inert but leaks otherwise.
                            return this.deliveryManager.delete(collected.parcel())
                                .exceptionally(throwable -> {
                                    LOGGER.log(Level.WARNING, "Failed to delete stray delivery for purged parcel "
                                        + collected.parcel(), throwable);
                                    return false;
                                });
                        });
                }
                // Stray row: the parcel is gone or is a live shipment again — drop only the row.
                return this.collectedParcelRepository.delete(collected.parcel());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Failed to purge expired collected parcel " + collected.parcel()
                    + " (will retry next run)", throwable);
                return false;
            });
    }
}
