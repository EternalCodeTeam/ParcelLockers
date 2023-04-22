package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.shared.Position;
import io.sentry.Sentry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelLockerRepositoryJdbcImpl implements ParcelLockerRepository {

    private final JdbcConnectionProvider provider;

    private ParcelLockerRepositoryJdbcImpl(JdbcConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public CompletableFuture<Void> save(ParcelLocker parcelLocker) {
        return CompletableFuture.runAsync(() -> this.provider.executeUpdate("INSERT INTO `parcelLockers` (`uuid`, `description`, `position`, `size`) VALUES (" + parcelLocker.getUuid().toString() + ", " + parcelLocker.getDescription() + ", " + parcelLocker.getPosition().toString() + ")"))
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (ResultSet resultSet = this.provider.executeQuery("SELECT * FROM `parcelLockers` WHERE `uuid` = " + uuid.toString())) {
                if (resultSet.next()) {
                    UUID parcelLockerUUID = UUID.fromString(resultSet.getString("uuid"));
                    String description = resultSet.getString("description");
                    Position position = Position.parse(resultSet.getString("position"));
                    ParcelLocker parcelLocker = new ParcelLocker(parcelLockerUUID, description, position);
                    return Optional.of(parcelLocker);
                }
                else {
                    return Optional.empty();
                }

            }
            catch (SQLException exception) {
                Sentry.captureException(exception);
                throw new RuntimeException(exception);
            }

        });
    }

    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByPosition(Position position) {
        return CompletableFuture.supplyAsync(() -> {
            try (ResultSet resultSet = this.provider.executeQuery("SELECT * FROM `parcelLockers` WHERE `position` = " + position.toString())) {
                if (resultSet.next()) {
                    UUID parcelLockerUUID = UUID.fromString(resultSet.getString("uuid"));
                    String description = resultSet.getString("description");
                    Position parcelLockerPosition = Position.parse(resultSet.getString("position"));
                    ParcelLocker parcelLocker = new ParcelLocker(parcelLockerUUID, description, parcelLockerPosition);
                    return Optional.of(parcelLocker);
                }
                else {
                    return Optional.empty();
                }

            }
            catch (SQLException exception) {
                Sentry.captureException(exception);
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
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    String description = resultSet.getString("description");
                    Position position = Position.parse(resultSet.getString("position"));
                    ParcelLocker parcelLocker = new ParcelLocker(uuid, description, position);
                    results.add(parcelLocker);
                }
                return results;
            }
            catch (SQLException exception) {
                Sentry.captureException(exception);
                throw new RuntimeException(exception);
            }

        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            this.provider.executeUpdate("DELETE FROM `parcelLockers` WHERE `uuid` = " + uuid.toString());

        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public CompletableFuture<Void> remove(ParcelLocker parcelLocker) {
        return this.remove(parcelLocker.getUuid());
    }

    public static ParcelLockerRepositoryJdbcImpl create(JdbcConnectionProvider provider) {
        try {
            provider.executeUpdate("CREATE TABLE IF NOT EXISTS `parcelLockers` (`uuid` VARCHAR(36) NOT NULL, `description` VARCHAR(255) NOT NULL, `position` VARCHAR(255) NOT NULL, PRIMARY KEY (`uuid`))");
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        return new ParcelLockerRepositoryJdbcImpl(provider);
    }
}
