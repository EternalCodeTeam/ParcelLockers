package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UserManagerImpl implements UserManager {

    private final UserRepository userRepository;

    private final Cache<UUID, User> usersByUUID = Caffeine.newBuilder()
        .expireAfterAccess(2, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build();

    private final Cache<String, User> usersByName = Caffeine.newBuilder()
        .expireAfterAccess(2, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build();

    public UserManagerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CompletableFuture<Optional<User>> get(UUID uniqueId) {
        User user = this.usersByUUID.getIfPresent(uniqueId);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.userRepository.fetch(uniqueId);
    }

    @Override
    public CompletableFuture<Optional<User>> get(String username) {
        User user = this.usersByName.getIfPresent(username);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.userRepository.fetch(username);
    }

    @Override
    public void getOrCreate(UUID uuid, String name) {
        User userByUUID = this.usersByUUID.getIfPresent(uuid);

        if (userByUUID != null) {
            return;
        }

        User userByName = this.usersByName.getIfPresent(name);

        if (userByName != null) {
            return;
        }

        this.create(uuid, name);
    }

    @Override
    public CompletableFuture<PageResult<User>> getPage(Page page) {
        return this.userRepository.fetchPage(page);
    }

    @Override
    public User create(UUID uuid, String name) {
        if (this.usersByUUID.getIfPresent(uuid) != null || this.usersByName.getIfPresent(name) != null) {
            throw new IllegalStateException("User already exists");
        }

        User user = new User(uuid, name);
        this.usersByUUID.put(uuid, user);
        this.usersByName.put(name, user);
        this.userRepository.save(user);

        return user;
    }
}
