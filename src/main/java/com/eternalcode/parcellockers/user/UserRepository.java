package com.eternalcode.parcellockers.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    CompletableFuture<Void> save(User user);

    CompletableFuture<Optional<User>> findByName(String name);

    CompletableFuture<Optional<User>> findByUuid(UUID uuid);

    CompletableFuture<List<User>> findAll();

    CompletableFuture<Void> remove(User user);

    CompletableFuture<Void> remove(UUID uuid);
}
