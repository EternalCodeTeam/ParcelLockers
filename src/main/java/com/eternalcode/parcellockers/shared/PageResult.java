package com.eternalcode.parcellockers.shared;

import java.util.List;

public record PageResult<T>(List<T> items, boolean hasNextPage) {

    private static final PageResult<?> EMPTY = new PageResult<>(List.of(), false);

    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> empty() {
        return (PageResult<T>) EMPTY;
    }
}
