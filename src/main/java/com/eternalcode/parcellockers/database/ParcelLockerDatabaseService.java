package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerPageResult;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import io.sentry.Sentry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelLockerDatabaseService implements ParcelLockerRepository {

    private final Map<UUID, ParcelLocker> cache = new HashMap<>();
    private final Map<Position, UUID> positionCache = new HashMap<>();

    private final DataSource dataSource;

    public ParcelLockerDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;

        this.initTable();
    }

    private void initTable() {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `parcellockers`(" +
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
                this.cache.put(parcelLocker.uuid(), parcelLocker);
                this.positionCache.put(parcelLocker.position(), parcelLocker.uuid());
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
        return this.findBy("uuid", uuid.toString());
    }

    @Override
    public CompletableFuture<Optional<ParcelLocker>> findByPosition(Position position) {
        return this.findBy("position", position.toString());
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
                this.cache.remove(uuid);
                this.positionCache.remove(this.cache.get(uuid).position());
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
    public CompletableFuture<ParcelLockerPageResult> findPage(Page page) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM `parcellockers` LIMIT ? OFFSET ?"
                 )
            ) {
                statement.setInt(1, page.getLimit());
                statement.setInt(2, page.getOffset());
                Set<ParcelLocker> parcelLockers = this.extractParcelLockers(statement);

                try (PreparedStatement statement1 = connection.prepareStatement(
                    "SELECT * FROM `parcellockers` LIMIT 1 OFFSET ?"
                )) {
                    statement1.setInt(1, page.getLimit() + page.getOffset());
                    ResultSet rs1 = statement1.executeQuery();
                    boolean hasNext = rs1.next();

                    return new ParcelLockerPageResult(parcelLockers, hasNext);
                }
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
        set.forEach(parcelLocker -> {
            this.cache.put(parcelLocker.uuid(), parcelLocker);
            this.positionCache.put(parcelLocker.position(), parcelLocker.uuid());
        });
        return set;
    }

    public Optional<ParcelLocker> findLocker(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    public Map<UUID, ParcelLocker> cache() {
        return Collections.unmodifiableMap(this.cache);
    }

    public Map<Position, UUID> positionCache() {
        return Collections.unmodifiableMap(this.positionCache);
    }

    public CompletableFuture<Void> updatePositionCache() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM `parcellockers` WHERE `position` IS NOT NULL;"
                 )
            ) {
                ResultSet rs = statement.executeQuery();

                this.positionCache.clear();
                
                while (rs.next()) {
                   Position position = Position.parse(rs.getString("position")); 
                   UUID uuid = UUID.fromString(rs.getString("uuid"));
                   
                    this.positionCache.put(position, uuid);
                }
            } catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    private CompletableFuture<Optional<ParcelLocker>> findBy(String column, String value) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM `parcellockers` WHERE `" + column + "` = ?;"
                 )
            ) {
                statement.setString(1, value);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    ParcelLocker parcelLocker = new ParcelLocker(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("description"),
                            Position.parse(rs.getString("position"))
                    );
                    this.cache.put(parcelLocker.uuid(), parcelLocker);
                    return Optional.of(parcelLocker);
                }
                return Optional.<ParcelLocker>empty();
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }
}
