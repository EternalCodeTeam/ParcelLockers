package com.eternalcode.parcellockers.locker.validation;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import java.util.Optional;
import java.util.UUID;

public interface LockerValidationService {

    ValidationResult validateCreateParameters(UUID uniqueId, String name, Position position);

    ValidationResult validateNoConflicts(UUID uniqueId, Position position,
        Optional<Locker> existingByUUID,
        Optional<Locker> existingByPosition);
}
