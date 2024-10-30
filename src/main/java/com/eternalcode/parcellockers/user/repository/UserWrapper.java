package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.parcellockers.user.User;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "users")
class UserWrapper {

    @DatabaseField(id = true)
    private UUID uuid;

    @DatabaseField
    private String username;

    UserWrapper() {
    }

    UserWrapper(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    static UserWrapper from(User user) {
        return new UserWrapper(user.uuid(), user.name());
    }

    public void setUsername(String username) {
        this.username = username;
    }

    User toUser() {
        return new User(this.uuid, this.username);
    }
}
