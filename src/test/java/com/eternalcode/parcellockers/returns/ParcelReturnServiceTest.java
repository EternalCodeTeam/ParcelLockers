package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParcelReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");
    private static final Duration WINDOW = Duration.ofDays(7);

    @Test
    void withinWindowJustAfterCollection() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofHours(1)));
        assertTrue(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void outsideWindowAfterExpiry() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(Duration.ofDays(8)));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }

    @Test
    void exactExpiryInstantIsOutsideWindow() {
        CollectedParcel collected = new CollectedParcel(UUID.randomUUID(), NOW.minus(WINDOW));
        assertFalse(ParcelReturnService.isWithinReturnWindow(collected, WINDOW, NOW));
    }
}
