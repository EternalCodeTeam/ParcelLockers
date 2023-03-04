package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.ParcelLockers;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.database.LastLoginStorage;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryJdbcImpl implements UserRepository {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final PluginConfiguration config;

    private UserRepositoryJdbcImpl(JdbcConnectionProvider jdbcConnectionProvider, PluginConfiguration config) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.config = config;
    }

    @Override
    public void save(Player user) {
        this.jdbcConnectionProvider.executeUpdate("INSERT INTO `users` (`uuid`, `name`, `lastLogin`) VALUES (" + user.getUniqueId() + "," + user.getName() + "," + LastLoginStorage.lastLoginMap.get(user.getUniqueId()).toEpochMilli() + " ) ON DUPLICATE KEY UPDATE `name` = ?, `lastLogin` = ?");
    }

    @Override
    public Optional<User> findByName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) {

    }

    @SneakyThrows
    @Override
    public List<User> findAll() {

    }

    @SneakyThrows
    public static UserRepositoryJdbcImpl create(JdbcConnectionProvider jdbcConnectionProvider) {
        jdbcConnectionProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(24) NOT NULL, `lastLogin` VARCHAR(64), PRIMARY KEY (`uuid`))");

        return new UserRepositoryJdbcImpl(jdbcConnectionProvider, ParcelLockers.getInstance().getPluginConfig());
    }

}
