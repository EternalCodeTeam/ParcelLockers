package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.ParcelLockers;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        this.jdbcConnectionProvider.executeUpdate("INSERT INTO `users` (`uuid`, `name`) VALUES (" + user.getUniqueId() + "," + user.getName() + ") ON DUPLICATE KEY UPDATE `name` = ?");
    }

    @SneakyThrows
    @Override
    public Optional<User> findByName(String name) {
        Connection connection = this.jdbcConnectionProvider.createConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE `name` = ? LIMIT 1".replace("?", name));
        ResultSet resultSet = statement.getResultSet();

        return Optional.of(new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), null));
    }

    @SneakyThrows
    @Override
    public Optional<User> findByUuid(UUID uuid) {
        Connection connection = this.jdbcConnectionProvider.createConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE `uuid` = ? LIMIT 1".replace("?", uuid.toString()));
        ResultSet resultSet = statement.getResultSet();

        return Optional.of(new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), null));
    }

    @SneakyThrows
    @Override
    public List<User> findAll() {
        ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users`");
        ParcelRepositoryJdbcImpl parcelRepository = ParcelRepositoryJdbcImpl.create(this.jdbcConnectionProvider);

        Set<Parcel> parcelList = parcelRepository.findAll();
        List<User> users = new ArrayList<>();

        while (resultSet.next()) {
            Set<UUID> userParcels = new HashSet<>();
            for (Parcel parcel : parcelList) {
                if (parcel.getSender().equals(UUID.fromString(resultSet.getString("uuid")))) {
                    userParcels.add(parcel.getUuid());
                }
            }
            users.add(new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), userParcels));

        }
        return users;
    }

    @SneakyThrows
    public static UserRepositoryJdbcImpl create(JdbcConnectionProvider jdbcConnectionProvider) {
        jdbcConnectionProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(24) NOT NULL, `lastLogin` VARCHAR(64), PRIMARY KEY (`uuid`))");

        return new UserRepositoryJdbcImpl(jdbcConnectionProvider, ParcelLockers.getInstance().getPluginConfig());
    }

}
