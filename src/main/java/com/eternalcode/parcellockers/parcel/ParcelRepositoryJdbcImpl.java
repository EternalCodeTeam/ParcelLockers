package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;

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
            this.jdbcConnectionProvider.executeUpdate("INSERT INTO `parcels` (`uuid`, `name`, `description`, `priority`, `receiver`, `size`, `entryLocker`, `destinationLocker`, `sender`) VALUES (%u, %n, %d, %p, %r, %s, %e, %dst, %sn)"
                    .replace("%u", parcel.getUuid().toString())
                    .replace("%n", parcel.getMeta().getName())
                    .replace("%d", parcel.getMeta().getDescription())
                    .replace("%p", String.valueOf(parcel.getMeta().isPriority()))
                    .replace("%r", parcel.getMeta().getReceiver().toString())
                    .replace("%s", parcel.getMeta().getSize().name())
                    .replace("%e", parcel.getMeta().getEntryLocker().getUuid().toString())
                    .replace("%dst", parcel.getMeta().getDestinationLocker().getUuid().toString())
                    .replace("%sn", parcel.getSender().toString()));
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels` WHERE `uuid` = " + uuid)) {
                if (resultSet.next()) {
                    Parcel parcel = extractParcel(parcelLockerRepository, resultSet);

                    return Optional.of(parcel);
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Set<Parcel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            ParcelLockerRepositoryJdbcImpl parcelLockerRepository = ParcelLockerRepositoryJdbcImpl.create(this.jdbcConnectionProvider);
            Set<Parcel> parcels = new HashSet<>();

            try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels`")) {
                while (resultSet.next()) {
                    Parcel parcel = extractParcel(parcelLockerRepository, resultSet);
                    parcels.add(parcel);
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return parcels;
        });
    }

    @Override
    public CompletableFuture<Void> remove(Parcel parcel) {
        return CompletableFuture.runAsync(() -> {
            this.jdbcConnectionProvider.executeUpdate("DELETE FROM `parcels` WHERE `uuid` = " + parcel.getUuid().toString());
        }).orTimeout(5, TimeUnit.SECONDS);
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
