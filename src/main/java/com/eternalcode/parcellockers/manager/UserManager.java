package com.eternalcode.parcellockers.manager;

import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.UserRepository;

import java.util.List;
import java.util.UUID;

public class UserManager {

    private final UserRepository repository;

    public UserManager(UserRepository repository) {
        this.repository = repository;
    }

    public void save(User user) {
        this.repository.save(user);
    }

    public User findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid).join().orElseThrow(() -> new IllegalArgumentException("User with UUID " + uuid + " does not exist!"));
    }

    public void remove(User user) {
        this.repository.remove(user);
    }

    public void remove(UUID uuid) {
        this.repository.remove(uuid);
    }

    public List<User> findAll() {
        return this.repository.findAll().join();
    }

}
