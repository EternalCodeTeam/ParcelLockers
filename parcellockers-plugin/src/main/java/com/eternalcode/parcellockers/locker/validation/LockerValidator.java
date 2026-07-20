package com.eternalcode.parcellockers.locker.validation;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.shared.validation.ValidationRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LockerValidator implements LockerValidationService {

    private final List<ValidationRule<CreateLockerRequest>> creationRules;
    private final LockerConflictDetectionStrategy conflictStrategy;

    public LockerValidator() {
        this.creationRules = List.of(
            this::validateUUID,
            this::validateName,
            this::validatePosition);

        this.conflictStrategy = this::defaultConflictDetection;
    }

    public LockerValidator(List<ValidationRule<CreateLockerRequest>> creationRules,
        LockerConflictDetectionStrategy conflictStrategy) {
        this.creationRules = creationRules;
        this.conflictStrategy = conflictStrategy;
    }

    @Override
    public ValidationResult validateCreateParameters(UUID uniqueId, String name, Position position) {
        CreateLockerRequest request = new CreateLockerRequest(uniqueId, name, position);

        return this.creationRules.stream()
            .reduce(ValidationRule::and)
            .map(rule -> rule.validate(request))
            .orElse(ValidationResult.valid());
    }

    @Override
    public ValidationResult validateNoConflicts(UUID uniqueId, Position position,
        Optional<Locker> existingByUUID,
        Optional<Locker> existingByPosition) {
        return this.conflictStrategy.detectConflicts(uniqueId, position, existingByUUID, existingByPosition);
    }

    private ValidationResult validateUUID(CreateLockerRequest request) {
        return request.uniqueId() != null ?
            ValidationResult.valid() :
            ValidationResult.invalid("UUID cannot be null");
    }

    private ValidationResult validateName(CreateLockerRequest request) {
        return request.name() != null && !request.name().trim().isEmpty() ?
            ValidationResult.valid() :
            ValidationResult.invalid("Name cannot be empty");
    }

    private ValidationResult validatePosition(CreateLockerRequest request) {
        return request.position() != null ?
            ValidationResult.valid() :
            ValidationResult.invalid("Position cannot be null");
    }

    private ValidationResult defaultConflictDetection(
        UUID uniqueId, Position position,
        Optional<Locker> existingByUUID,
        Optional<Locker> existingByPosition) {
        return existingByUUID.flatMap(byUUID -> existingByPosition.map(byPosition -> this.validateBothExist(uniqueId, position, byUUID, byPosition)))
            .orElseGet(() -> existingByUUID
                .map(locker -> this.validateUUIDConflict(uniqueId, position, locker))
                .orElseGet(() -> existingByPosition
                    .map(locker -> this.validatePositionConflict(uniqueId, position, locker))
                    .orElse(ValidationResult.valid())));
    }

    private ValidationResult validateBothExist(UUID uniqueId, Position position,
        Locker byUUID, Locker byPosition) {
        if (!byUUID.equals(byPosition)) {
            return ValidationResult.invalid(String.format("Conflicting lockers found. UUID %s vs Position %s",
                uniqueId, position));
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateUUIDConflict(UUID uniqueId, Position position, Locker locker) {
        if (!locker.position().equals(position)) {
            return ValidationResult.invalid(String.format("Locker with UUID %s exists at different position: %s",
                uniqueId, locker.position()));
        }

        return ValidationResult.valid();
    }

    private ValidationResult validatePositionConflict(UUID uniqueId, Position position, Locker locker) {
        if (!locker.uuid().equals(uniqueId)) {
            return ValidationResult.invalid(String.format("Locker at position %s has different UUID: %s",
                position, locker.uuid()));
        }

        return ValidationResult.valid();
    }

    private record CreateLockerRequest(
        UUID uniqueId,
        String name,
        Position position) {
    }
}
