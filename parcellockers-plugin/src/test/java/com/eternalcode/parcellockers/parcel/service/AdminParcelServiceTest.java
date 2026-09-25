package com.eternalcode.parcellockers.parcel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AdminParcelServiceTest {

    private static Parcel collectedParcel() {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "name", "description", false,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED);
    }

    @Test
    void changeStatusRefusesCollectedParcelWithoutTouchingAnyCollaborator() {
        // No collaborator is a usable mock/instance here on purpose: the COLLECTED guard must
        // short-circuit before dereferencing any of them, so a regression that removes the guard
        // and reaches a collaborator call would fail this test with an NPE instead of passing.
        AdminParcelService service = new AdminParcelService(null, null, null, null, null, null);

        Parcel collected = collectedParcel();
        EditResult result = service.changeStatus(collected, ParcelStatus.SENT).join();

        assertEquals(EditResult.Status.PARCEL_COLLECTED, result.status());
    }

    @Test
    void editOfStaleSnapshotIsRefusedWhenStatusChangedMeanwhile() {
        Parcel staleSent = new Parcel(UUID.randomUUID(), UUID.randomUUID(), "name", "description", false,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.SENT);
        ParcelService parcelService = mock(ParcelService.class);
        when(parcelService.updateIfStatus(any(), any())).thenReturn(CompletableFuture.completedFuture(false));
        AdminParcelService service = new AdminParcelService(parcelService, null, null, null, null, null);

        EditResult result = service.changeName(staleSent, "edited").join();

        assertEquals(EditResult.Status.PARCEL_COLLECTED, result.status());
        verify(parcelService).updateIfStatus(any(), eq(ParcelStatus.SENT));
        verify(parcelService, never()).update(any());
    }

    @Test
    void capacityMatchesContentGuiUsableSlots() {
        assertEquals(9, ParcelSize.SMALL.capacity());
        assertEquals(18, ParcelSize.MEDIUM.capacity());
        assertEquals(27, ParcelSize.LARGE.capacity());
    }

    @Test
    void enablingPriorityShortensDeliveryByDelta() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(5)); // normal delivery scheduled in 5 min
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, false, true, normal, priority, now);

        // delta = priority - normal = -4 min; oldTs - 4 min = now + 1 min
        assertEquals(now.plus(Duration.ofMinutes(1)), shifted);
    }

    @Test
    void disablingPriorityExtendsDeliveryByDelta() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(1));
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, true, false, normal, priority, now);

        // delta = normal - priority = +4 min; oldTs + 4 min = now + 5 min
        assertEquals(now.plus(Duration.ofMinutes(5)), shifted);
    }

    @Test
    void overdueShiftIsClampedToNow() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofSeconds(30)); // 30s left
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        // enabling priority: delta = -4 min, oldTs - 4 min is in the past -> clamp to now
        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, false, true, normal, priority, now);

        assertEquals(now, shifted);
    }

    @Test
    void unchangedPriorityKeepsTimestamp() {
        Instant now = Instant.parse("2026-06-21T12:00:00Z");
        Instant oldTs = now.plus(Duration.ofMinutes(3));
        Duration normal = Duration.ofMinutes(5);
        Duration priority = Duration.ofMinutes(1);

        Instant shifted = AdminParcelService.shiftedDeliveryTimestamp(oldTs, true, true, normal, priority, now);

        assertEquals(oldTs, shifted);
    }
}
