package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelRepositoryJdbcImpl;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    public CompletableFuture<Optional<User>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users` WHERE `name` = ? LIMIT 1".replace("?", name))) {
                return extractUser(resultSet);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<User>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users` WHERE `uuid` = ? LIMIT 1".replace("?", uuid.toString()))) {
                return extractUser(resultSet);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<List<User>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            Set<Parcel> parcelSet = this.parcelRepository.findAll().join();
            List<User> users = new ArrayList<>();

            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `users`")) {

                while (resultSet.next()) {
                    Set<UUID> userParcels = new HashSet<>();

                    for (Parcel parcel : parcelSet) {
                        UUID target = UUID.fromString(resultSet.getString("uuid"));

                        if (parcel.getSender().equals(target)) {
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
        });
    }

    @Override
    public CompletableFuture<Void> remove(User user) {
        return CompletableFuture.runAsync(() ->
                this.jdbcConnectionProvider.executeUpdate("DELETE FROM `users` WHERE `uuid` = ?".replace("?", user.getUuid().toString())))
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() ->
                this.jdbcConnectionProvider.executeUpdate("DELETE FROM `users` WHERE `uuid` = ?".replace("?", uuid.toString())))
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @NotNull
    private Optional<User> extractUser(ResultSet resultSet) throws SQLException {
        Set<Parcel> parcelSet = this.parcelRepository.findAll().join();
        Set<UUID> userParcels = new HashSet<>();

        while (resultSet.next()) {
            for (Parcel parcel : parcelSet) {
                UUID target = UUID.fromString(resultSet.getString("uuid"));

                if (parcel.getSender().equals(target)) {
                    userParcels.add(parcel.getUuid());
                }
            }
        }

        if (resultSet.next()) {
            User user = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"), userParcels);
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }

    public static UserRepositoryJdbcImpl create(JdbcConnectionProvider jdbcConnectionProvider) {
        jdbcConnectionProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(24) NOT NULL, PRIMARY KEY (`uuid`))");

        return new UserRepositoryJdbcImpl(jdbcConnectionProvider);
    }

}
