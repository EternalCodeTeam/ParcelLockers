package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    CompletableFuture<Void> save(User user);

    CompletableFuture<Void> changeName(UUID uuid, String newName);

    CompletableFuture<Optional<User>> find(UUID uuid);

    CompletableFuture<Optional<User>> find(String name);

    CompletableFuture<UserPageResult> findPage(Page page);
}
