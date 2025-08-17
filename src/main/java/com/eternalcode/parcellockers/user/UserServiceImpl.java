package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final Map<UUID, User> usersByUUID = new HashMap<>();
    private final Map<String, User> usersByName = new HashMap<>();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CompletableFuture<Optional<User>> get(UUID uniqueId) {
        User user = this.usersByUUID.get(uniqueId);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.userRepository.find(uniqueId);
    }

    @Override
    public CompletableFuture<Optional<User>> get(String username) {
        User user = this.usersByName.get(username);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.userRepository.find(username);
    }

    @Override
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

    @Override
    public User create(UUID uuid, String name) {
        if (this.usersByUUID.containsKey(uuid) || this.usersByName.containsKey(name)) {
            throw new IllegalStateException("User already exists");
        }

        User user = new User(uuid, name);
        this.usersByUUID.put(uuid, user);
        this.usersByName.put(name, user);
        this.userRepository.save(user);

        return user;
    }
}
