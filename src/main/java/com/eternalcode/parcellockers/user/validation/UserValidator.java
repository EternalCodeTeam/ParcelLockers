package com.eternalcode.parcellockers.user.validation;

import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.shared.validation.ValidationRule;
import com.eternalcode.parcellockers.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class UserValidator implements UserValidationService {

    private final List<ValidationRule<CreateUserRequest>> creationRules;
    private final UserConflictDetectionStrategy conflictStrategy;

    public UserValidator() {
        this.creationRules = List.of(
            this::validateUUID,
            this::validateName
        );

        this.conflictStrategy = this::defaultConflictDetection;
    }

    public UserValidator(List<ValidationRule<CreateUserRequest>> creationRules,
        UserConflictDetectionStrategy conflictStrategy) {
        this.creationRules = creationRules;
        this.conflictStrategy = conflictStrategy;
    }

    @Override
    public ValidationResult validateCreateParameters(UUID uniqueId, String name) {
        CreateUserRequest request = new CreateUserRequest(uniqueId, name);

        return this.creationRules.stream()
            .reduce(ValidationRule::and)
            .map(rule -> rule.validate(request))
            .orElse(ValidationResult.valid());
    }

    @Override
    public ValidationResult validateNoConflicts(UUID uniqueId, String username,
        Optional<User> existingByUUID,
        Optional<User> existingByUsername) {
        return this.conflictStrategy.detectConflicts(uniqueId, username, existingByUUID, existingByUsername);
    }

    private ValidationResult validateUUID(CreateUserRequest request) {
        return request.uniqueId() != null ?
            ValidationResult.valid() :
            ValidationResult.invalid("UUID cannot be null");
    }

    private ValidationResult validateName(CreateUserRequest request) {
        return request.name() != null && !request.name().trim().isEmpty() ?
            ValidationResult.valid() :
            ValidationResult.invalid("Name cannot be empty");
    }

    private ValidationResult defaultConflictDetection(
        UUID uniqueId, String username,
        Optional<User> existingByUUID,
        Optional<User> existingByUsername) {
        return existingByUUID.flatMap(byUUID -> existingByUsername.map(byUsername -> this.validateBothExist(uniqueId, username, byUUID, byUsername)))
            .orElseGet(() -> existingByUUID
                .map(user -> this.validateUUIDConflict(uniqueId, username, user))
                .orElseGet(() -> existingByUsername
                    .map(user -> this.validateUsernameConflict(uniqueId, username, user))
                    .orElse(ValidationResult.valid())));
    }

    private ValidationResult validateBothExist(UUID uniqueId, String username,
        User byUUID, User byUsername) {
        if (!byUUID.equals(byUsername)) {
            return ValidationResult.invalid(String.format("Conflicting users found. UUID %s vs username %s",
                uniqueId, username));
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateUUIDConflict(UUID uniqueId, String username, User user) {
        if (!user.name().equals(username)) {
            return ValidationResult.invalid(String.format("User with UUID %s exists at different username: %s",
                uniqueId, username));
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateUsernameConflict(UUID uniqueId, String username, User user) {
        if (!user.uuid().equals(uniqueId)) {
            return ValidationResult.invalid(String.format("User with username %s exists at different UUID: %s",
                username, uniqueId));
        }

        return ValidationResult.valid();
    }

    private record CreateUserRequest(
        UUID uniqueId,
        String name
    ) {
    }
}
