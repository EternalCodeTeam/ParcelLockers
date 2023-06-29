package com.eternalcode.parcellockers.parcellocker;

import com.eternalcode.parcellockers.shared.Position;

import java.util.UUID;

public record ParcelLocker(UUID uuid, String description, Position position) {

}
