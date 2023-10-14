package com.eternalcode.parcellockers.locker;

import com.eternalcode.parcellockers.shared.Position;

import java.util.UUID;

public record Locker(UUID uuid, String description, Position position) {

}
