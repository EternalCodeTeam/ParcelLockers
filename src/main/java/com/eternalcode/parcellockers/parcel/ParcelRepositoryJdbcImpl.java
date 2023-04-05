package com.eternalcode.parcellockers.parcel;


import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParcelRepositoryJdbcImpl implements ParcelRepository {

    private final JdbcConnectionProvider jdbcConnectionProvider;

    private ParcelRepositoryJdbcImpl(JdbcConnectionProvider jdbcConnectionProvider) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
    }

    @Override
    public void save(Parcel parcel) {
        this.jdbcConnectionProvider.executeUpdate("INSERT INTO `parcels` (`uuid`, `name`, `description`, `priority`, `receiver`, `size`, `entryLocker`, `destinationLocker`, `sender`) VALUES ("
                + parcel.getUuid()
                + "," + parcel.getMeta().getName()
                + "," + parcel.getMeta().getDescription()
                + "," + parcel.getMeta().isPriority()
                + "," + parcel.getMeta().getReceiver()
                + "," + parcel.getMeta().getSize().name()
                + "," + parcel.getMeta().getEntryLocker()
                + "," + parcel.getMeta().getDestinationLocker()
                + "," + parcel.getSender() + " )");
    }

    @Override
    public Optional<Parcel> findByUuid(UUID uuid) {
        return Optional.empty();
    }

    @SneakyThrows
    @Override
    public List<Parcel> findAll() {
        ResultSet resultSet = this.jdbcConnectionProvider.executeQuery("SELECT * FROM `parcels`");
        return new Parcel(
                UUID.fromString(resultSet.getString("uuid")),
                UUID.fromString(resultSet.getString("sender")),
                new ParcelMeta(
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getBoolean("priority"),
                        UUID.fromString(resultSet.getString("receiver")),
                        ParcelSize.valueOf(resultSet.getString("size")),
                        new ParcelRepositoryJdbcImpl(this.jdbcConnectionProvider).findByUuid(UUID.fromString(resultSet.getString("entryLocker"))).get().getMeta().getEntryLocker(),
                        new ParcelRepositoryJdbcImpl(this.jdbcConnectionProvider).findByUuid(UUID.fromString(resultSet.getString("destinationLocker"))).get().getMeta().getDestinationLocker(
                        )
                ));
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
