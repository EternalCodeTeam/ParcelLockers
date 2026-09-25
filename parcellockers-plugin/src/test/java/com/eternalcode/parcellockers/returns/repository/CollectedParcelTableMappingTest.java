package com.eternalcode.parcellockers.returns.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.j256.ormlite.field.DatabaseField;
import org.junit.jupiter.api.Test;

class CollectedParcelTableMappingTest {

    @Test
    void keepsHistoricalSnakeCaseCollectedAtColumn() throws NoSuchFieldException {
        DatabaseField mapping = CollectedParcelTable.class
            .getDeclaredField("collectedAt")
            .getAnnotation(DatabaseField.class);

        assertEquals("collected_at", mapping.columnName());
        assertEquals("collected_at", CollectedParcelTable.COLLECTED_AT_COLUMN);
    }
}
