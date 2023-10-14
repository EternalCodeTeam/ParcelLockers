package com.eternalcode.parcellockers.deliverycode;

import java.util.UUID;

public record DeliveryCode(UUID parcelUUID, String code) {
}
