package com.eternalcode.parcellockers.delivery;

import java.util.UUID;

public record Delivery(UUID parcel, long deliveryTimestamp) {

}
