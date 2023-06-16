package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import io.sentry.Sentry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelDatabaseService implements ParcelRepository {

    private final Set<Parcel> cache = new HashSet<>();

    private final DataSource dataSource;
    private final ParcelLockerRepository parcelLockerRepository;

    public ParcelDatabaseService(DataSource dataSource, ParcelLockerRepository parcelLockerRepository) {
        this.dataSource = dataSource;
        this.parcelLockerRepository = parcelLockerRepository;

        this.initTable();
    }

    private void initTable() {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `parcels`(" +
                             "`uuid` VARCHAR(36) NOT NULL, " +
                             "`name` VARCHAR(24) NOT NULL, " +
                             "`description` VARCHAR(64), " +
                             "`priority` BOOLEAN NOT NULL, " +
                             "`receiver` VARCHAR(36) NOT NULL, " +
                             "`size` VARCHAR(10) NOT NULL, " +
                             "`entryLocker` VARCHAR(36) NOT NULL, " +
                             "`destinationLocker` VARCHAR(36) NOT NULL, " +
                             "`sender` VARCHAR(36) NOT NULL, " +
                             "PRIMARY KEY (uuid) " +
                             ");"
             )
        ) {
            statement.execute();
        }
        catch (SQLException e) {
            throw new ParcelLockersException(e);
        }
    }

    @Override
    public CompletableFuture<Void> save(Parcel parcel) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `parcels`(uuid, " +
                                    "`name`, " +
                                    "`description`, " +
                                    "`priority`, " +
                                    "`receiver`, " +
                                    "`size`, " +
                                    "`entryLocker`, " +
                                    "`destinationLocker`, " +
                                    "`sender`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
            ) {
                statement.setString(1, parcel.uuid().toString());
                statement.setString(2, parcel.name());
                statement.setString(3, parcel.description());
                statement.setBoolean(4, parcel.priority());
                statement.setString(5, parcel.receiver().toString());
                statement.setString(6, parcel.size().name());
                statement.setString(7, parcel.entryLocker().toString());
                statement.setString(8, parcel.destinationLocker().toString());
                statement.setString(9, parcel.sender().toString());
                statement.execute();
               this.cache.add(parcel);

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> update(Parcel oldParcel, Parcel newParcel) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE `parcels` SET " +
                                 "`name` = ?, " +
                                 "`description` = ?, " +
                                 "`priority` = ?, " +
                                 "`receiver` = ?, " +
                                 "`size` = ?, " +
                                 "`entryLocker` = ?, " +
                                 "`destinationLocker` = ?, " +
                                 "`sender` = ? " +
                                 "WHERE `uuid` = ?"
                 )
            ) {
                statement.setString(1, newParcel.name());
                statement.setString(2, newParcel.description());
                statement.setBoolean(3, newParcel.priority());
                statement.setString(4, newParcel.receiver().toString());
                statement.setString(5, newParcel.size().name());
                statement.setString(6, newParcel.entryLocker().toString());
                statement.setString(7, newParcel.destinationLocker().toString());
                statement.setString(8, newParcel.sender().toString());
                statement.setString(9, oldParcel.uuid().toString());
                statement.execute();

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `parcels` WHERE `uuid` = ?")) {
                statement.setString(1, uuid.toString());
                ResultSet rs = statement.executeQuery();
                Parcel parcel = null;
                if (rs.next()) {
                    parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            new HashSet<>(),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            UUID.fromString(rs.getString("entryLocker")),
                            UUID.fromString(rs.getString("destinationLocker"))
                    );
                    this.cache.add(parcel);
                }
                return Optional.ofNullable(parcel);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<Parcel>> findBySender(UUID sender) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcels` WHERE `sender` = ?"
                 )
            ) {
                statement.setString(1, sender.toString());
                ResultSet rs = statement.executeQuery();
                Set<Parcel> parcels = new HashSet<>();
                while (rs.next()) {
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            new HashSet<>(),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            UUID.fromString(rs.getString("entryLocker")),
                            UUID.fromString(rs.getString("destinationLocker"))
                    );
                    this.cache.add(parcel);
                    parcels.add(parcel);
                }
                return parcels;

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<Parcel>> findByReceiver(UUID receiver) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcels` WHERE `receiver` = ?"
                 )
            ) {
                statement.setString(1, receiver.toString());
                ResultSet rs = statement.executeQuery();
                Set<Parcel> parcels = new HashSet<>();

                while (rs.next()) {
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            new HashSet<>(),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            UUID.fromString(rs.getString("entryLocker")),
                            UUID.fromString(rs.getString("destinationLocker"))
                    );
                    parcels.add(parcel);
                    this.cache.add(parcel);
                }
                return parcels;

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<Parcel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcels`"
                 )
            ) {
                Set<Parcel> parcels = new HashSet<>();
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            new HashSet<>(),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            UUID.fromString(rs.getString("entryLocker")),
                            UUID.fromString(rs.getString("destinationLocker"))
                    );
                    parcels.add(parcel);
                }
                this.cache.clear();
                this.cache.addAll(parcels);
                return parcels;

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(Parcel parcel) {
        return this.remove(parcel.uuid());
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM `parcels` WHERE `uuid` = ?"
                 )
            ) {
                statement.setString(1, uuid.toString());
                statement.execute();
                this.getCache().removeIf(parcel -> parcel.uuid().equals(uuid));

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<Parcel>> findPage(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcels` LIMIT ? OFFSET ?"
                 )
            ) {
                statement.setInt(1, pageSize);
                statement.setInt(2, page * pageSize);
                Set<Parcel> parcels = new HashSet<>();
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    Parcel parcel = new Parcel(
                            UUID.fromString(rs.getString("uuid")),
                            UUID.fromString(rs.getString("sender")),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("priority"),
                            new HashSet<>(),
                            UUID.fromString(rs.getString("receiver")),
                            ParcelSize.valueOf(rs.getString("size")),
                            UUID.fromString(rs.getString("entryLocker")),
                            UUID.fromString(rs.getString("destinationLocker"))
                    );
                    parcels.add(parcel);
                }
                this.getCache().addAll(parcels);
                return parcels;

            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    public Set<Parcel> getCache() {
        return this.cache;
    }

    public Parcel getFromCache(UUID uuid) {
        return this.cache.stream()
                .filter(parcel -> parcel.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }
}
