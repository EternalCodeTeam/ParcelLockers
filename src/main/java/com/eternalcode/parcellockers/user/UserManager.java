package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {

    CompletableFuture<User> create(UUID uuid, String name);

    CompletableFuture<User> getOrCreate(UUID uuid, String name);

    CompletableFuture<Optional<User>> get(String username);

    CompletableFuture<Optional<User>> get(UUID uniqueId);

    CompletableFuture<PageResult<User>> getPage(Page page);
}
