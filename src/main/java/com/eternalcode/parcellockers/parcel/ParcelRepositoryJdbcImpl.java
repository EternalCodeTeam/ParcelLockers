package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelRepositoryJdbcImpl implements ParcelRepository {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    private ParcelRepositoryJdbcImpl(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        return CompletableFuture.runAsync(() -> {
            this.jdbcConnectionProvider.executeUpdate("INSERT INTO `parcels` (`uuid`, `name`, `description`, `priority`, `receiver`, `size`, `entryLocker`, `destinationLocker`, `sender`) VALUES ("
                    + parcel.getUuid()
                    + "," + parcel.getMeta().getName()
                    + "," + parcel.getMeta().getDescription()
                    + "," + parcel.getMeta().isPriority()
                    + "," + parcel.getMeta().getReceiver()
                    + "," + parcel.getMeta().getSize().name()
                    + "," + parcel.getMeta().getEntryLocker().getUuid()
                    + "," + parcel.getMeta().getDestinationLocker().getUuid()
                    + "," + parcel.getSender() + " )");
        });
    }

    @Override
    public Optional<Parcel> findByUuid(UUID uuid) {

        try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels` WHERE `uuid` = " + uuid)) {
            if (resultSet.next()) {
                return Optional.of(new Parcel(
                        UUID.fromString(resultSet.getString("uuid")),
                        UUID.fromString(resultSet.getString("sender")),
                        new ParcelMeta(
                                resultSet.getString("name"),
                                resultSet.getString("description"),
                                resultSet.getBoolean("priority"),
                                UUID.fromString(resultSet.getString("receiver")),
                                ParcelSize.valueOf(resultSet.getString("size")),
                                this.findByUuid(UUID.fromString(resultSet.getString("entryLocker"))).get().getMeta().getEntryLocker(),
                                this.findByUuid(UUID.fromString(resultSet.getString("destinationLocker"))).get().getMeta().getDestinationLocker()
                        )));
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
        return Optional.empty();
    }

    @Override
    public Set<Parcel> findAll() {
        Set<Parcel> parcels = new HashSet<>();

        try (ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels`")) {
            while (resultSet.next()) {
                parcels.add(new Parcel(
                        UUID.fromString(resultSet.getString("uuid")),
                        UUID.fromString(resultSet.getString("sender")),
                        new ParcelMeta(
                                resultSet.getString("name"),
                                resultSet.getString("description"),
                                resultSet.getBoolean("priority"),
                                UUID.fromString(resultSet.getString("receiver")),
                                ParcelSize.valueOf(resultSet.getString("size")),
                                new ParcelRepositoryJdbcImpl(this.jdbcConnectionProvider).findByUuid(UUID.fromString(resultSet.getString("entryLocker"))).get().getMeta().getEntryLocker(),
                                new ParcelRepositoryJdbcImpl(this.jdbcConnectionProvider).findByUuid(UUID.fromString(resultSet.getString("destinationLocker"))).get().getMeta().getDestinationLocker()
                        )));
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
        return parcels;
    }

    @SneakyThrows
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

}
