package com.eternalcode.parcellockers.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    User create(UUID uuid, String name);

    User getOrCreate(UUID uuid, String name);

    CompletableFuture<Optional<User>> get(String username);

    CompletableFuture<Optional<User>> get(UUID uniqueId);
}
