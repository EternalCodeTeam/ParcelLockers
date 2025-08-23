package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.repository.UserPageResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {

    User create(UUID uuid, String name);

    void getOrCreate(UUID uuid, String name);

    CompletableFuture<Optional<User>> get(String username);

    CompletableFuture<Optional<User>> get(UUID uniqueId);

    CompletableFuture<UserPageResult> getPage(Page page);
}
