package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import com.eternalcode.parcellockers.user.event.UserChangeNameEvent;
import com.eternalcode.parcellockers.user.event.UserCreateEvent;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.user.validation.UserValidationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.okaeri.configs.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Server;

public class UserManagerImpl implements UserManager {

    private final UserRepository userRepository;
    private final UserValidationService validationService;
    private final Server server;

    private final Cache<UUID, User> usersByUUID;
    private final Cache<String, User> usersByName;

    public UserManagerImpl(UserRepository userRepository, UserValidationService validationService, Server server) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.server = server;
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

        return this.userRepository.fetch(uniqueId).thenApply(optional -> {
            optional.ifPresent(this::cache);
            return optional;
        });
    }

    @Override
    public CompletableFuture<Optional<User>> get(String username) {
        User user = this.usersByName.getIfPresent(username);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.userRepository.fetch(username).thenApply(optional -> {
            optional.ifPresent(this::cache);
            return optional;
        });
    }

    private void cache(User user) {
        this.usersByUUID.put(user.uuid(), user);
        this.usersByName.put(user.name(), user);
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

        // Not cached - consult the repository before creating, otherwise a user that exists in the
        // database but not the cache would be created a second time.
        return this.userRepository.fetch(uuid).thenCompose(optional -> {
            if (optional.isPresent()) {
                User user = optional.get();
                this.cache(user);
                return CompletableFuture.completedFuture(user);
            }
            return this.create(uuid, name);
        });
    }

    @Override
    public CompletableFuture<PageResult<User>> getPage(Page page) {
        return this.userRepository.fetchPage(page);
    }

    @Override
    public CompletableFuture<User> create(UUID uuid, String name) {
        ValidationResult validation = this.validationService.validateCreateParameters(uuid, name);

        if (!validation.isValid()) {
            return CompletableFuture.failedFuture(
                new ValidationException("Invalid user parameters: " + validation.errorMessage()));
        }

        // Check for conflicts against the database, not only the cache, so a name/UUID already
        // persisted but not currently cached is still detected.
        return this.userRepository.fetch(uuid).thenCombine(this.userRepository.fetch(name),
            (existingByUUID, existingByName) -> {
                ValidationResult conflictCheck = this.validationService.validateNoConflicts(
                    uuid, name, existingByUUID, existingByName);

                if (!conflictCheck.isValid()) {
                    throw new ValidationException(conflictCheck.errorMessage());
                }

                return new User(uuid, name);
            }).thenCompose(user -> {
                UserCreateEvent event = new UserCreateEvent(user);
                this.server.getPluginManager().callEvent(event);

                this.cache(user);
                // Chain the save so a persistence failure is surfaced to the caller; if it fails, undo
                // the optimistic cache entry so the cache never holds an unpersisted user.
                return this.userRepository.save(user)
                    .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            this.usersByUUID.invalidate(uuid);
                            this.usersByName.invalidate(name);
                        }
                    })
                    .thenApply(ignored -> user);
            });
    }

    @Override
    public CompletableFuture<Void> changeName(UUID uuid, String newName) {
        User cached = this.usersByUUID.getIfPresent(uuid);
        CompletableFuture<Optional<User>> lookup = cached != null
            ? CompletableFuture.completedFuture(Optional.of(cached))
            : this.userRepository.fetch(uuid);

        // thenComposeAsync keeps the body (including the async UserChangeNameEvent) off the main thread
        // even on a cache hit, where the lookup completes synchronously.
        return lookup.thenComposeAsync(optional -> {
            User oldUser = optional.orElseThrow(
                () -> new ValidationException("User not found with UUID: " + uuid));
            String oldName = oldUser.name();

            UserChangeNameEvent event = new UserChangeNameEvent(oldUser, oldName);
            this.server.getPluginManager().callEvent(event);

            // Optimistically update the cache, reverting if the persist fails.
            User updatedUser = new User(uuid, newName);
            this.usersByUUID.put(uuid, updatedUser);
            this.usersByName.invalidate(oldName);
            this.usersByName.put(newName, updatedUser);

            return this.userRepository.changeName(uuid, newName)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        this.usersByUUID.put(uuid, oldUser);
                        this.usersByName.invalidate(newName);
                        this.usersByName.put(oldName, oldUser);
                    }
                });
        });
    }
}
