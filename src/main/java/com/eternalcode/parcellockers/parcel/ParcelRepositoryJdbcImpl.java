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
            ParcelMeta meta = parcel.getMeta();
            this.jdbcConnectionProvider.executeUpdate("INSERT INTO `parcels` (`uuid`, `name`, `description`, `priority`, `receiver`, `size`, `entryLocker`, `destinationLocker`, `sender`) VALUES (%u, %n, %d, %p, %r, %s, %e, %dst, %sn)"
                    .replace("%u", parcel.getUuid().toString())
                    .replace("%n", meta.getName())
                    .replace("%d", meta.getDescription())
                    .replace("%p", String.valueOf(meta.isPriority()))
                    .replace("%r", meta.getReceiver().toString())
                    .replace("%s", meta.getSize().name())
                    .replace("%e", meta.getEntryLocker().getUuid().toString())
                    .replace("%dst", meta.getDestinationLocker().getUuid().toString())
                    .replace("%sn", parcel.getSender().toString()));
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> update(Parcel oldParcel, Parcel newParcel) {
        return CompletableFuture.runAsync(() -> {
            ParcelMeta meta = newParcel.getMeta();
            this.jdbcConnectionProvider.executeUpdate("UPDATE `parcels` SET `name` = %n, `description` = %d, `priority` = %p, `receiver` = %r, `size` = %s, `entryLocker` = %e, `destinationLocker` = %dst, `sender` = %sn WHERE `uuid` = %u"
                    .replace("%u", oldParcel.getUuid().toString())
                    .replace("%n", meta.getName())
                    .replace("%d", meta.getDescription())
                    .replace("%p", String.valueOf(meta.isPriority()))
                    .replace("%r", meta.getReceiver().toString())
                    .replace("%s", meta.getSize().name())
                    .replace("%e", meta.getEntryLocker().getUuid().toString())
                    .replace("%dst", meta.getDestinationLocker().getUuid().toString())
                    .replace("%sn", newParcel.getSender().toString()));
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
                throw new RuntimeException(exception);
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
                throw new RuntimeException(exception);
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
                throw new RuntimeException(exception);
            }
            return parcels;
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(Parcel parcel) {
        return this.remove(parcel.getUuid());
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
