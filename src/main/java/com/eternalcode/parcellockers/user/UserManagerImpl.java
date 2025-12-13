package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.user.validation.UserValidationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.okaeri.configs.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UserManagerImpl implements UserManager {

    private final UserRepository userRepository;
    private final UserValidationService validationService;

    private final Cache<UUID, User> usersByUUID;
    private final Cache<String, User> usersByName;

    public UserManagerImpl(UserRepository userRepository, UserValidationService validationService) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.usersByUUID = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();
        this.usersByName = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();
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
    public CompletableFuture<User> getOrCreate(UUID uuid, String name) {
        User userByUUID = this.usersByUUID.getIfPresent(uuid);

        if (userByUUID != null) {
            return CompletableFuture.completedFuture(userByUUID);
        }

        User userByName = this.usersByName.getIfPresent(name);

        if (userByName != null) {
            return CompletableFuture.completedFuture(userByName);
        }

        return this.create(uuid, name);
    }

    @Override
    public CompletableFuture<PageResult<User>> getPage(Page page) {
        return this.userRepository.fetchPage(page);
    }

    @Override
    public CompletableFuture<User> create(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            ValidationResult validation = this.validationService.validateCreateParameters(uuid, name);

            if (!validation.isValid()) {
                throw new ValidationException("Invalid user parameters: " + validation.errorMessage());
            }

            Optional<User> existingByUUID = Optional.ofNullable(this.usersByUUID.get(uuid, uniqueId -> null));
            Optional<User> existingByName = Optional.ofNullable(this.usersByName.get(name, username -> null));

            ValidationResult conflictCheck = this.validationService.validateNoConflicts(
                uuid, name, existingByUUID, existingByName);

            if (!conflictCheck.isValid()) {
                throw new ValidationException(conflictCheck.errorMessage());
            }

            User user = new User(uuid, name);
            this.usersByUUID.put(uuid, user);
            this.usersByName.put(name, user);
            this.userRepository.save(user);

            return user;
        });
    }
}
