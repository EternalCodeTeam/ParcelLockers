package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcel.ParcelLocker;
import com.eternalcode.parcellockers.shared.Position;
import io.sentry.Sentry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelLockerDatabaseService {

    private final DataSource dataSource;

    public ParcelLockerDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void initTable() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS parcelLockers(" +
                             "uuid VARCHAR(36) NOT NULL, " +
                             "description VARCHAR(64) NOT NULL " +
                             "position VARCHAR(255) NOT NULL" +
                             "PRIMARY KEY (uuid)" +
                             ");"
             )
        ) {
            statement.execute();
        }
        catch (SQLException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException(e);
        }
    }

    public void save(ParcelLocker parcelLocker) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO parcelLockers(uuid, " +
                                 "description, " +
                                 "position" +
                                 ") VALUES(?, ?, ?);"
                 )
            ) {
                statement.setString(1, parcelLocker.getUuid().toString());
                statement.setString(2, parcelLocker.getDescription());
                statement.setString(3, parcelLocker.getPosition().toString());
                statement.execute();
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void findAll(List<ParcelLocker> emptyList) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM parcelLockers;"
                 )
            ) {
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    ParcelLocker parcelLocker = new ParcelLocker(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("description"),
                            Position.parse(rs.getString("position"))
                    );
                    emptyList.add(parcelLocker);
                }
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void remove(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM parcelLockers WHERE uuid = ?;"
                 )
            ) {
                statement.setString(1, uuid.toString());
                statement.execute();
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void remove(ParcelLocker parcelLocker) {
        this.remove(parcelLocker.getUuid());
    }

    // TODO: Find by UUID, find by position

}
