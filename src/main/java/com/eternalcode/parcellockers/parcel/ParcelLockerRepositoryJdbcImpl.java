package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.shared.Position;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelLockerRepositoryJdbcImpl implements ParcelLockerRepository {

    private final JdbcConnectionProvider provider;

    public ParcelLockerRepositoryJdbcImpl(JdbcConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public CompletableFuture<Void> save(ParcelLocker parcelLocker) {
        return CompletableFuture.runAsync(() -> this.provider.executeUpdate("INSERT INTO `parcelLockers` (`uuid`, `description`, `position`, `size`) VALUES (" + parcelLocker.getUuid().toString() + ", " + parcelLocker.getDescription() + ", " + parcelLocker.getPosition().toString() + ")"));
    }

    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (ResultSet resultSet = this.provider.executeQuery("SELECT * FROM `parcelLockers` WHERE `uuid` = " + uuid.toString())) {
                if (resultSet.next()) {
                    ParcelLocker parcelLocker = new ParcelLocker(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("description"),
                            Position.parse(resultSet.getString("position"))
                    );
                    return Optional.of(parcelLocker);
                } else {
                    return Optional.empty();
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<List<ParcelLocker>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<ParcelLocker> results = new ArrayList<>();

            try (ResultSet resultSet = this.provider.executeQuery("SELECT * FROM `parcelLockers`")) {
                while (resultSet.next()) {

                    ParcelLocker parcelLocker = new ParcelLocker(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("description"),
                            Position.parse(resultSet.getString("position"))
                    );
                    results.add(parcelLocker);
                }
                return results;
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public static ParcelLockerRepositoryJdbcImpl create(JdbcConnectionProvider provider) {
        provider.executeUpdate("CREATE TABLE IF NOT EXISTS `parcelLockers` (`uuid` VARCHAR(36) NOT NULL, `description` VARCHAR(255) NOT NULL, `position` VARCHAR(255) NOT NULL, PRIMARY KEY (`uuid`))");
        return new ParcelLockerRepositoryJdbcImpl(provider);
    }
}
