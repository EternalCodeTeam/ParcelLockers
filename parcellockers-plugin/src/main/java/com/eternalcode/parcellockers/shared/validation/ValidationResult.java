package com.eternalcode.parcellockers.shared.validation;

public record ValidationResult(
    boolean isValid,
    String errorMessage
) {

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
}
