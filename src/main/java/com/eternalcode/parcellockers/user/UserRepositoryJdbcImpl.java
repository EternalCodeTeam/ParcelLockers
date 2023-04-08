package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserRepositoryJdbcImpl implements UserRepository {

    private final JdbcConnectionProvider jdbcConnectionProvider;
    private final ParcelRepositoryJdbcImpl parcelRepository;

    private UserRepositoryJdbcImpl(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.parcelRepository = ParcelRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
    }

    @Override
    public CompletableFuture<Void> save(User user) {
        return CompletableFuture.runAsync(() ->
                this.jdbcConnectionProvider.executeUpdate("INSERT INTO `users` (`uuid`, `name`) VALUES (" + user.getUuid() + "," + user.getName() + ") ON DUPLICATE KEY UPDATE `name` = ?"));
    }

    @Override
    public Optional<User> findByName(String name) {
        try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users` WHERE `name` = ? LIMIT 1".replace("?", name))) {
            Set<Parcel> parcelSet = this.parcelRepository.findAll();
            Set<UUID> userParcels = new HashSet<>();
            while (resultSet.next()) {
                for (Parcel parcel : parcelSet) {
                    if (parcel.getSender().equals(UUID.fromString(resultSet.getString("uuid")))) {
                        userParcels.add(parcel.getUuid());
                    }
                }
            }
            User user = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), userParcels);
            
            return Optional.of(user);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) {
        try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users` WHERE `uuid` = ? LIMIT 1".replace("?", uuid.toString()))) {
            Set<Parcel> parcelSet = this.parcelRepository.findAll();
            Set<UUID> userParcels = new HashSet<>();
            while (resultSet.next()) {
                for (Parcel parcel : parcelSet) {
                    if (parcel.getSender().equals(UUID.fromString(resultSet.getString("uuid")))) {
                        userParcels.add(parcel.getUuid());
                    }
                }
            }
            User user = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), userParcels);

            return Optional.of(user);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public List<User> findAll() {
        Set<Parcel> parcelSet = this.parcelRepository.findAll();
        List<User> users = new ArrayList<>();

        try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users`")) {
            while (resultSet.next()) {
                Set<UUID> userParcels = new HashSet<>();
                for (Parcel parcel : parcelSet) {
                    if (parcel.getSender().equals(UUID.fromString(resultSet.getString("uuid")))) {
                        userParcels.add(parcel.getUuid());
                    }
                }
                User user = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), userParcels);

                users.add(user);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
        return users;
    }

    public static UserRepositoryJdbcImpl create(JdbcConnectionProvider jdbcConnectionProvider) {
        jdbcConnectionProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(24) NOT NULL, `lastLogin` VARCHAR(64), PRIMARY KEY (`uuid`))");

        return new UserRepositoryJdbcImpl(jdbcConnectionProvider);
    }

}
