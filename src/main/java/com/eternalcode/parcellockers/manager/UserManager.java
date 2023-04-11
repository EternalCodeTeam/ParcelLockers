package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserManager {

    private final UserRepository repository;

    public UserManager(UserRepository repository) {
        this.repository = repository;
    }

    // TODO add real methods

    public void save(User user) {
        this.repository.save(user);
    }

    public Optional<User> findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid).join();
    }

    public void remove(UUID uuid) {
        this.repository.remove(uuid);
    }

    public List<User> findAll() {
        return this.repository.findAll().join();
    }

}
