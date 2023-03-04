package com.eternalcode.parcellockers.user;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    void save(Player user);

    Optional<User> findByName(String name);

    Optional<User> findByUuid(UUID uuid);

    List<User> findAll();
}
