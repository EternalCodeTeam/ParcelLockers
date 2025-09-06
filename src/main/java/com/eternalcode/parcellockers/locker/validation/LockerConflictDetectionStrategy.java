package com.eternalcode.parcellockers.locker.validation;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface LockerConflictDetectionStrategy {

    ValidationResult detectConflicts(
        UUID uniqueId, Position position,
        Optional<Locker> existingByUUID,
        Optional<Locker> existingByPosition);
}