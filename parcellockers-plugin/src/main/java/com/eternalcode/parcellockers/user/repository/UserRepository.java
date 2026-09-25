package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    CompletableFuture<Void> save(User user);

    CompletableFuture<Void> changeName(UUID uuid, String newName);

    CompletableFuture<Optional<User>> fetch(UUID uuid);

    CompletableFuture<Optional<User>> fetch(String name);

    CompletableFuture<PageResult<User>> fetchPage(Page page);
}
