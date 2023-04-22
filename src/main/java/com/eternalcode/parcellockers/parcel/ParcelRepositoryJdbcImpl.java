package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import io.sentry.Sentry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelRepositoryJdbcImpl implements ParcelRepository {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    private ParcelRepositoryJdbcImpl(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        return CompletableFuture.runAsync(() -> {
            ParcelMeta meta = parcel.meta();
            try (PreparedStatement statement = this.jdbcConnectionProvider.createConnection().prepareStatement("INSERT INTO `parcels` (`uuid`, `name`, `description`, `priority`, `receiver`, `size`, `entryLocker`, `destinationLocker`, `sender`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")){
                statement.setString(1, parcel.uuid().toString());statement.setString(2, meta.getName());
                statement.setString(3, meta.getDescription());
                statement.setBoolean(4, meta.isPriority());
                statement.setString(5, meta.getReceiver().toString());
                statement.setString(6, meta.getSize().name());
                statement.setString(7, meta.getEntryLocker().getUuid().toString());
                statement.setString(8, meta.getDestinationLocker().getUuid().toString());
                statement.setString(9, parcel.sender().toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                exception.printStackTrace();
                Sentry.captureException(exception);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> update(Parcel oldParcel, Parcel newParcel) {
        return CompletableFuture.runAsync(() -> {
            ParcelMeta meta = newParcel.meta();
            try (PreparedStatement statement = this.jdbcConnectionProvider.createConnection().prepareStatement("UPDATE `parcels` SET `name` = ?, `description` = ?, `priority` = ?, `receiver` = ?, `size` = ?, `entryLocker` = ?, `destinationLocker` = ?, `sender` = ? WHERE `uuid` = ?")){
                statement.setString(1, meta.getName());
                statement.setString(2, meta.getDescription());
                statement.setBoolean(3, meta.isPriority());
                statement.setString(4, meta.getReceiver().toString());
                statement.setString(5, meta.getSize().name());
                statement.setString(6, meta.getEntryLocker().getUuid().toString());
                statement.setString(7, meta.getDestinationLocker().getUuid().toString());
                statement.setString(8, newParcel.sender().toString());
                statement.setString(9, oldParcel.uuid().toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                exception.printStackTrace();
                Sentry.captureException(exception);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels` WHERE `uuid` = " + uuid)) {
                if (resultSet.next()) {
                    Parcel parcel = this.extractParcel(parcelLockerRepository, resultSet);

                    return Optional.of(parcel);
                }
            }
            catch (SQLException exception) {
                exception.printStackTrace();
                Sentry.captureException(exception);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Set<Parcel>> findBySender(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
            Set<Parcel> parcels = new HashSet<>();

            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels` WHERE `sender` = " + uuid)) {
                while (resultSet.next()) {
                    Parcel parcel = this.extractParcel(parcelLockerRepository, resultSet);
                    parcels.add(parcel);
                }
            }
            catch (SQLException exception) {
                exception.printStackTrace();
                Sentry.captureException(exception);
            }
            return parcels;
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<Parcel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
            Set<Parcel> parcels = new HashSet<>();

            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels`")) {
                while (resultSet.next()) {
                    Parcel parcel = this.extractParcel(parcelLockerRepository, resultSet);
                    parcels.add(parcel);
                }
            }
            catch (SQLException exception) {
                exception.printStackTrace();
                Sentry.captureException(exception);
            }
            return parcels;
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(Parcel parcel) {
        return this.remove(parcel.uuid());
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            this.jdbcConnectionProvider.executeUpdate("DELETE FROM `parcels` WHERE `uuid` = " + uuid.toString());
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public static ParcelRepositoryJdbcImpl create(JdbcConnectionProvider jdbcConnectionProvider) {
        jdbcConnectionProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `parcels` (`uuid` VARCHAR(36) NOT NULL," +
                " `name` VARCHAR(24) NOT NULL," +
                " `description` VARCHAR(64)," +
                " `priority` VARCHAR(5) NOT NULL," +
                " `receiver` VARCHAR(36) NOT NULL," +
                " `size` VARCHAR(10) NOT NULL," +
                " `entryLocker` VARCHAR(36) NOT NULL," +
                " `destinationLocker` VARCHAR(36) NOT NULL," +
                " `sender` VARCHAR(36) NOT NULL," +
                "  PRIMARY KEY (`uuid`))");
        return new ParcelRepositoryJdbcImpl(jdbcConnectionProvider);
    }

    private Parcel extractParcel(ParcelLockerRepositoryJdbcImpl parcelLockerRepository, ResultSet resultSet) throws SQLException {
        ParcelMeta meta = new ParcelMeta(
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBoolean("priority"),
                UUID.fromString(resultSet.getString("receiver")),
                ParcelSize.valueOf(resultSet.getString("size")),
                parcelLockerRepository.findByUuid(UUID.fromString(resultSet.getString("entryLocker"))).join().get(),
                parcelLockerRepository.findByUuid(UUID.fromString(resultSet.getString("destinationLocker"))).join().get());

        return new Parcel(
                UUID.fromString(resultSet.getString("uuid")),
                UUID.fromString(resultSet.getString("sender")),
                meta);
    }

}
