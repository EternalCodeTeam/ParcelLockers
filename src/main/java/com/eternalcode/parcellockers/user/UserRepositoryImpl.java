package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserRepositoryImpl extends AbstractDatabaseService implements UserRepository {

    private final Map<UUID, User> usersByUUID = new HashMap<>();
    private final Map<String, User> usersByName = new HashMap<>();

    public UserRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(UUID uuid) {
        User user = this.usersByUUID.get(uuid);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(String name) {
        User user = this.usersByName.get(name);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Void> save(User user) {
        this.usersByUUID.put(user.uuid(), user);
        this.usersByName.put(user.name(), user);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> changeName(UUID uuid, String newName) {
        User user = this.usersByUUID.get(uuid);

        if (user == null) {
            return CompletableFuture.completedFuture(null);
        }

        this.usersByName.remove(user.name());
        user = new User(uuid, newName);
        this.usersByName.put(newName, user);
        this.usersByUUID.put(uuid, user);

        return CompletableFuture.completedFuture(null);
    }

}
