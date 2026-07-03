package com.eternalcode.parcellockers.returns.repository;

import com.eternalcode.parcellockers.database.persister.InstantPersister;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.time.Instant;
import java.util.UUID;

@DatabaseTable(tableName = "collected_parcels")
class CollectedParcelTable {

    static final String COLLECTED_AT_COLUMN = "collected_at";

    @DatabaseField(id = true)
    private UUID parcel;

    @DatabaseField(columnName = COLLECTED_AT_COLUMN, persisterClass = InstantPersister.class)
    private Instant collectedAt;

    CollectedParcelTable() {
    }

    CollectedParcelTable(UUID parcel, Instant collectedAt) {
        this.parcel = parcel;
        this.collectedAt = collectedAt;
    }

    static CollectedParcelTable from(CollectedParcel collectedParcel) {
        return new CollectedParcelTable(collectedParcel.parcel(), collectedParcel.collectedAt());
    }

    CollectedParcel toCollectedParcel() {
        return new CollectedParcel(this.parcel, this.collectedAt);
    }
}
