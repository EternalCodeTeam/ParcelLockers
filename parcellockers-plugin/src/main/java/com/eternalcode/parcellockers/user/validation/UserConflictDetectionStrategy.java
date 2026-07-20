package com.eternalcode.parcellockers.user.validation;

import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.user.User;
import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface UserConflictDetectionStrategy {

    ValidationResult detectConflicts(
        UUID uuid, String username,
        Optional<User> existingByUUID,
        Optional<User> existingByUsername);
}
