package com.eternalcode.parcellockers.delivery;

import java.time.Instant;
import java.util.UUID;

public record Delivery(UUID parcel, Instant deliveryTimestamp) {

}
