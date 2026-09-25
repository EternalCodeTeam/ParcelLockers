package com.eternalcode.parcellockers.shared.validation;

@FunctionalInterface
public interface ValidationRule<T> {

    default ValidationRule<T> and(ValidationRule<T> other) {
        return value -> {
            ValidationResult result = this.validate(value);
            return result.isValid() ? other.validate(value) : result;
        };
    }
    ValidationResult validate(T value);
}
