package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import com.eternalcode.parcellockers.shared.Position;
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

public class ParcelLockerDatabaseService implements ParcelLockerRepository {

    private final Set<ParcelLocker> cache = new HashSet<>();

    private final DataSource dataSource;

    public ParcelLockerDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;

        this.initTable();
    }

    private void initTable() {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `parcelLockers`(" +
                             "uuid VARCHAR(36) NOT NULL, " +
                             "description VARCHAR(64) NOT NULL, " +
                             "position VARCHAR(255) NOT NULL, " +
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

    @Override
    public CompletableFuture<Void> save(ParcelLocker parcelLocker) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO `parcellockers`(`uuid`, " +
                                 "`description`, " +
                                 "`position`" +
                                 ") VALUES(?, ?, ?);"
                 )
            ) {
                statement.setString(1, parcelLocker.uuid().toString());
                statement.setString(2, parcelLocker.description());
                statement.setString(3, parcelLocker.position().toString());
                statement.execute();
                this.cache.add(parcelLocker);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Set<ParcelLocker>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcellockers`;"
                 )
            ) {
                return this.extractParcelLockers(statement);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }


    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByUUID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcellockers` WHERE `uuid` = ?;"
                 )
            ) {
                statement.setString(1, uuid.toString());
                ResultSet rs = statement.executeQuery();
                ParcelLocker parcelLocker = null;

                if (rs.next()) {
                    parcelLocker = new ParcelLocker(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("description"),
                            Position.parse(rs.getString("position"))
                    );
                }
                this.cache.add(parcelLocker);
                return Optional.ofNullable(parcelLocker);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByPosition(Position position) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcelLockers` WHERE `position` = ?;"
                 )
            ) {
                statement.setString(1, position.toString());
                ResultSet rs = statement.executeQuery();
                ParcelLocker parcelLocker = null;

                if (rs.next()) {
                    parcelLocker = new ParcelLocker(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("description"),
                            Position.parse(rs.getString("position"))
                    );
                }
                this.getCache().add(parcelLocker);
                return Optional.ofNullable(parcelLocker);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM `parcellockers` WHERE `uuid` = ?;"
                 )
            ) {
                statement.setString(1, uuid.toString());
                statement.execute();
                this.cache.removeIf(parcelLocker -> parcelLocker.uuid().equals(uuid));
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Void> remove(ParcelLocker parcelLocker) {
        return this.remove(parcelLocker.uuid());
    }

    @Override
    public CompletableFuture<Set<ParcelLocker>> findPage(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcellockers` LIMIT ? OFFSET ?;"
                 )
            ) {
                statement.setInt(1, pageSize);
                statement.setInt(2, page * pageSize);
                return this.extractParcelLockers(statement);
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    private Set<ParcelLocker> extractParcelLockers(PreparedStatement statement) throws SQLException {
        Set<ParcelLocker> set = new HashSet<>();
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            ParcelLocker parcelLocker = new ParcelLocker(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("description"),
                    Position.parse(rs.getString("position"))
            );
            set.add(parcelLocker);
        }
        this.getCache().clear();
        this.cache.addAll(set);
        return set;
    }

    public Set<ParcelLocker> getCache() {
        return this.cache;
    }

    public ParcelLocker getFromCache(UUID uuid) {
        return this.cache.stream()
                .filter(parcelLocker -> parcelLocker.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }


}
