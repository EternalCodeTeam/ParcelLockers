package com.eternalcode.parcellockers.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserManager {

    private final UserRepository userRepository;

    private final Map<UUID, User> usersByUUID = new HashMap<>();
    private final Map<String, User> usersByName = new HashMap<>();

    public UserManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CompletableFuture<Optional<User>> getUser(UUID uuid) {
        User user = this.usersByUUID.get(uuid);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        // TODO fetch from database

        return CompletableFuture.completedFuture(Optional.empty());
    }

    public CompletableFuture<Optional<User>> getUser(String name) {
        User user = this.usersByName.get(name);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        // TODO fetch from database

        return CompletableFuture.completedFuture(Optional.empty());
    }

    public User getOrCreate(UUID uuid, String name) {
        User userByUUID = this.usersByUUID.get(uuid);

        if (userByUUID != null) {
            return userByUUID;
        }

        User userByName = this.usersByName.get(name);

        if (userByName != null) {
            return userByName;
        }

        return this.create(uuid, name);
    }

    public User create(UUID uuid, String name) {
        if (this.usersByUUID.containsKey(uuid) || this.usersByName.containsKey(name)) {
            throw new IllegalStateException("User already exists");
        }

        User user = new User(uuid, name);
        this.usersByUUID.put(uuid, user);
        this.usersByName.put(name, user);

        return user;
    }

    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(this.usersByUUID.values());
    }
}
