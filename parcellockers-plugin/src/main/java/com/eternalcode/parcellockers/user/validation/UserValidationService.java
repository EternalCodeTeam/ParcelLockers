package com.eternalcode.parcellockers.user.validation;

import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.user.User;
import java.util.Optional;
import java.util.UUID;

public interface UserValidationService {

    ValidationResult validateCreateParameters(UUID uniqueId, String username);

    ValidationResult validateNoConflicts(
        UUID uuid, String username,
        Optional<User> existingByUUID,
        Optional<User> existingByUsername);
}
