package com.eternalcode.parcellockers.parcel.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.DeliveryManager;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.task.ParcelSendTask.Decision;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ParcelSendTaskTest {

    @Test
    void deliversOnlyIfParcelIsStillSent() {
        Parcel sent = parcel(ParcelStatus.SENT);
        Delivery due = new Delivery(sent.uuid(), Instant.now().minusSeconds(1));
        ParcelService parcelService = mock(ParcelService.class);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);
        when(parcelService.get(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(sent)));
        when(deliveryManager.get(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(due)));
        when(parcelService.updateIfStatus(any(), eq(ParcelStatus.SENT)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(deliveryManager.delete(sent.uuid()))
            .thenReturn(CompletableFuture.completedFuture(true));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

            new ParcelSendTask(sent, parcelService, deliveryManager, mock(Scheduler.class)).run();
        }

        verify(parcelService).updateIfStatus(
            new Parcel(sent.uuid(), sent.sender(), sent.name(), sent.description(),
                sent.priority(), sent.receiver(), sent.size(), sent.entryLocker(),
                sent.destinationLocker(), ParcelStatus.DELIVERED),
            ParcelStatus.SENT);
        verify(parcelService, never()).update(any());
        verify(deliveryManager).delete(sent.uuid());
    }

    private static Parcel parcel(ParcelStatus status) {
        UUID id = UUID.randomUUID();
        return new Parcel(id, UUID.randomUUID(), "n", "d", false, UUID.randomUUID(),
            ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), status);
    }

    @Test
    void abortsWhenParcelMissing() {
        assertEquals(Decision.ABORT, ParcelSendTask.decide(Optional.empty(), Optional.empty(), Instant.now()));
    }

    @Test
    void abortsWhenAlreadyDelivered() {
        Parcel delivered = parcel(ParcelStatus.DELIVERED);
        assertEquals(Decision.ABORT, ParcelSendTask.decide(Optional.of(delivered), Optional.empty(), Instant.now()));
    }

    @Test
    void reschedulesWhenDeliveryMovedToFuture() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Parcel sent = parcel(ParcelStatus.SENT);
        Delivery future = new Delivery(sent.uuid(), now.plus(Duration.ofMinutes(2)));
        assertEquals(Decision.RESCHEDULE, ParcelSendTask.decide(Optional.of(sent), Optional.of(future), now));
    }

    @Test
    void deliversWhenDueAndStillSent() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Parcel sent = parcel(ParcelStatus.SENT);
        Delivery due = new Delivery(sent.uuid(), now.minus(Duration.ofSeconds(1)));
        assertEquals(Decision.DELIVER, ParcelSendTask.decide(Optional.of(sent), Optional.of(due), now));
    }

    @Test
    void deliversWhenSentWithNoDeliveryRow() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Parcel sent = parcel(ParcelStatus.SENT);
        assertEquals(Decision.DELIVER, ParcelSendTask.decide(Optional.of(sent), Optional.empty(), now));
    }

    @Test
    void abortsWhenAlreadyCollected() {
        // A COLLECTED parcel sits in the receiver's return window; a stale delivery row
        // must not re-deliver it, or the items could be collected twice.
        Parcel collected = parcel(ParcelStatus.COLLECTED);
        Delivery due = new Delivery(collected.uuid(), Instant.parse("2026-06-21T11:00:00Z"));
        assertEquals(Decision.ABORT, ParcelSendTask.decide(
            Optional.of(collected), Optional.of(due), Instant.parse("2026-06-21T12:00:00Z")));
    }
}
