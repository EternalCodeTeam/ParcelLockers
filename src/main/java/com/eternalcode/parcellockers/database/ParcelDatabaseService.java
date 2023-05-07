package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelMeta;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import io.sentry.Sentry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelDatabaseService {

    private final DataSource dataSource;

    public ParcelDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void initTable() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS parcels(" +
                             "uuid VARCHAR(36) NOT NULL, " +
                             "name VARCHAR(24) NOT NULL " +
                             "description VARCHAR(64)" +
                             "priority BOOLEAN NOT NULL" +
                             "receiver VARCHAR(36) NOT NULL" +
                             "size VARCHAR(10) NOT NULL" +
                             "entryLocker VARCHAR(36) NOT NULL" +
                             "destinationLocker VARCHAR(36) NOT NULL" +
                             "sender VARCHAR(36) NOT NULL" +
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

    public void save(Parcel parcel) {
        CompletableFuture.runAsync(() -> {
            ParcelMeta meta = parcel.meta();
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO parcels(uuid, " +
                                    "name, " +
                                    "description, " +
                                    "priority, " +
                                    "receiver, " +
                                    "size, " +
                                    "entryLocker, " +
                                    "destinationLocker, " +
                                    "sender) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
            ) {
                statement.setString(1, parcel.uuid().toString());
                statement.setString(2, meta.getName());
                statement.setString(3, meta.getDescription());
                statement.setBoolean(4, meta.isPriority());
                statement.setString(5, meta.getReceiver().toString());
                statement.setString(6, meta.getSize().name());
                statement.setString(7, meta.getEntryLocker().getUuid().toString());
                statement.setString(8, meta.getDestinationLocker().getUuid().toString());
                statement.setString(9, parcel.sender().toString());
                statement.execute();

            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void update(Parcel oldParcel, Parcel newParcel) {
        CompletableFuture.runAsync(() -> {
            ParcelMeta meta = newParcel.meta();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE parcels SET " +
                                 "name = ?, " +
                                 "description = ?, " +
                                 "priority = ?, " +
                                 "receiver = ?, " +
                                 "size = ?, " +
                                 "entryLocker = ?, " +
                                 "destinationLocker = ?, " +
                                 "sender = ? " +
                                 "WHERE uuid = ?"
                 )
            ) {
                statement.setString(1, meta.getName());
                statement.setString(2, meta.getDescription());
                statement.setBoolean(3, meta.isPriority());
                statement.setString(4, meta.getReceiver().toString());
                statement.setString(5, meta.getSize().name());
                statement.setString(6, meta.getEntryLocker().getUuid().toString());
                statement.setString(7, meta.getDestinationLocker().getUuid().toString());
                statement.setString(8, newParcel.sender().toString());
                statement.setString(9, oldParcel.uuid().toString());
                statement.execute();

            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void findBySender(UUID sender, Set<Parcel> emptySet) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM parcels WHERE sender = ?"
                 )
            ) {
                statement.setString(1, sender.toString());
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    ParcelMeta meta = new ParcelMeta(
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            null,
                            null
                    );
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            meta
                    );
                    emptySet.add(parcel);
                }

            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void findAll(Set<Parcel> emptySet) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM parcels"
                 )
            ) {
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    ParcelMeta meta = new ParcelMeta(
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            null,
                            null
                    );
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            meta
                    );
                    emptySet.add(parcel);
                }

            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public void remove(Parcel parcel) {
        this.remove(parcel.uuid());
    }

    public void remove(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM parcels WHERE uuid = ?"
                 )
            ) {
                statement.setString(1, uuid.toString());
                statement.execute();

            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    // TODO: findByUUID, findByReceiver?

}
