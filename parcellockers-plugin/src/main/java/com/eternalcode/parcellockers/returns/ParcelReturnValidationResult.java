package com.eternalcode.parcellockers.returns;

import java.util.List;

public record ParcelReturnValidationResult(List<ReturnItemMismatch> mismatches) {

    public ParcelReturnValidationResult {
        mismatches = List.copyOf(mismatches);
    }

    public boolean matches() {
        return this.mismatches.isEmpty();
    }
}
