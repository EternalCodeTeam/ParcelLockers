package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.database.JdbcConnectionProvider;
import com.eternalcode.parcellockers.util.LocationUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParcelLockerRepositoryJdbcImpl implements ParcelLockerRepository {

    private final JdbcConnectionProvider provider;

    public ParcelLockerRepositoryJdbcImpl(JdbcConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void save(ParcelLocker parcelLocker) {
        this.provider.executeUpdate("INSERT INTO `parcelLockers` (`uuid`, `description`, `location`, `size`) VALUES (" + parcelLocker.getUuid().toString() + ", " + parcelLocker.getDescription() + ", " + parcelLocker.getLocation().toString() + ")");
    }

    @Override
    public Optional<ParcelLocker> findByUuid(UUID uuid) {
        ResultSet resultSet = this.provider.executeQuery("SELECT * FROM `parcelLockers` WHERE `uuid` = " + uuid.toString());
        try {
            return Optional.of(new ParcelLocker(
                    UUID.fromString(resultSet.getString("uuid")),
                    resultSet.getString("description"),
                    LocationUtil.parseLocation(resultSet.getString("location"))
            ));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ParcelLocker> findAll() {
        return null;
    }

    public static ParcelLockerRepositoryJdbcImpl create(JdbcConnectionProvider provider) {
        provider.executeUpdate("CREATE TABLE IF NOT EXISTS `parcelLockers` (`uuid` VARCHAR(36) NOT NULL, `description` VARCHAR(255) NOT NULL, `location` VARCHAR(255) NOT NULL, PRIMARY KEY (`uuid`))");
        return new ParcelLockerRepositoryJdbcImpl(provider);
    }
}
