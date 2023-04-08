package com.eternalcode.parcellockers.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    CompletableFuture<Void> save(User user);

    Optional<User> findByName(String name);

    Optional<User> findByUuid(UUID uuid);

    List<User> findAll();
}
