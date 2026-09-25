package com.eternalcode.parcellockers.returns;

import java.time.Instant;
import java.util.UUID;

public record CollectedParcel(UUID parcel, Instant collectedAt) {
}
