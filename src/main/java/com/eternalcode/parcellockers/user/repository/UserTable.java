package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.parcellockers.user.User;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;

@DatabaseTable(tableName = "users")
class UserTable {

    @DatabaseField(id = true)
    private UUID uuid;

    @DatabaseField(index = true, unique = true, canBeNull = false)
    private String username;

    UserTable() {
    }

    UserTable(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    static UserTable from(User user) {
        return new UserTable(user.uuid(), user.name());
    }

    public void setUsername(String username) {
        this.username = username;
    }

    User toUser() {
        return new User(this.uuid, this.username);
    }
}
