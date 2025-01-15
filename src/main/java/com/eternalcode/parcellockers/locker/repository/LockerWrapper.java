package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.database.persister.PositionPersister;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.shared.Position;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "lockers")
class LockerWrapper {

    @DatabaseField(id = true, columnName = "uuid")
    private UUID uuid;

    @DatabaseField(columnName = "description")
    private String description;

    @DatabaseField(columnName = "position", persisterClass = PositionPersister.class, unique = true, index = true)
    private Position position;

    LockerWrapper() {
    }

    LockerWrapper(UUID uuid, String description, Position position) {
        this.uuid = uuid;
        this.description = description;
        this.position = position;
    }

    static LockerWrapper from(Locker locker) {
        return new LockerWrapper(locker.uuid(), locker.description(), locker.position());
    }

    Locker toLocker() {
        return new Locker(this.uuid, this.description, this.position);
    }
}
