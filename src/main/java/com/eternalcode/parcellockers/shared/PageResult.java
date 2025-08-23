package com.eternalcode.parcellockers.shared;

import java.util.List;

public record PageResult<T>(List<T> items, boolean hasNextPage) {

    public static PageResult empty() {
        return new PageResult(List.of(), false);
    }
}
