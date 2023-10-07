package com.eternalcode.parcellockers.locker.database;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerPageResult;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import io.sentry.Sentry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LockerDatabaseService extends AbstractDatabaseService implements LockerRepository {

    private final Map<UUID, Locker> cache = new HashMap<>();
    private final Map<Position, UUID> positionCache = new HashMap<>();

    public LockerDatabaseService(DataSource dataSource) {
        super(dataSource);

        this.initTable();
    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `lockers`(" +
                "uuid VARCHAR(36) NOT NULL, " +
                "description VARCHAR(64) NOT NULL, " +
                "position VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY (uuid)" +
                ");", PreparedStatement::execute);
    }

    @Override
    public CompletableFuture<Void> save(Locker locker) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO `lockers`(`uuid`, " +
                                 "`description`, " +
                                 "`position`" +
                                 ") VALUES(?, ?, ?);"
                 )
            ) {
                statement.setString(1, locker.uuid().toString());
                statement.setString(2, locker.description());
                statement.setString(3, locker.position().toString());
                statement.execute();
                this.cache.put(locker.uuid(), locker);
                this.positionCache.put(locker.position(), locker.uuid());
            }
            catch (SQLException e) {
                Sentry.captureException(e);
                throw new ParcelLockersException(e);
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<List<Locker>> findAll() {
        return this.supplyExecute("SELECT * FROM `lockers`;", this::extractParcelLockers);
    }


    @Override
    public CompletableFuture<Optional<Locker>> findByUUID(UUID uuid) {
        return this.findBy("uuid", uuid.toString());
    }

    @Override
    public CompletableFuture<Optional<Locker>> findByPosition(Position position) {
        return this.findBy("position", position.toString());
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return this.execute("DELETE FROM `lockers` WHERE `uuid` = ?;", statement -> {
            statement.setString(1, uuid.toString());
            statement.execute();
            
            this.removeFromCache(uuid);
        });
    }

    @Override
    public CompletableFuture<Void> remove(Locker locker) {
        return this.remove(locker.uuid());
    }

    @Override
    public CompletableFuture<LockerPageResult> findPage(Page page) {
        return this.supplyExecute("SELECT * FROM `lockers` LIMIT ? OFFSET ?;", statement -> {
            statement.setInt(1, page.getLimit() + 1);
            statement.setInt(2, page.getOffset());
            
            List<Locker> lockers = this.extractParcelLockers(statement);

            boolean hasNext = lockers.size() > page.getLimit();
            if (hasNext) {
                lockers.remove(lockers.size() - 1);
            }
            return new LockerPageResult(lockers, hasNext);
        });
    }

    private List<Locker> extractParcelLockers(PreparedStatement statement) throws SQLException {
        List<Locker> list = new ArrayList<>();
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            Locker locker = new Locker(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("description"),
                    Position.parse(rs.getString("position"))
            );

            list.add(locker);
        }

        list.forEach(this::addToCache);
        
        return list;
    }

    public Optional<Locker> findLocker(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    private void addToCache(Locker locker) {
        this.cache.put(locker.uuid(), locker);
        this.positionCache.put(locker.position(), locker.uuid());
    }

    private void removeFromCache(UUID uuid) {
        this.cache.remove(uuid);
        this.positionCache.remove(this.cache.get(uuid).position());
    }

    public Map<Position, UUID> positionCache() {
        return Collections.unmodifiableMap(this.positionCache);
    }

    public boolean isInCache(Position position) {
        return this.positionCache().containsKey(position);
    }

    public boolean isInCache(UUID uuid) {
        return this.cache.containsKey(uuid);
    }

    public CompletableFuture<Void> updatePositionCache() {
        return this.execute("SELECT * FROM `lockers` WHERE `position` IS NOT NULL;", statement -> {
            ResultSet rs = statement.executeQuery();

            this.positionCache.clear();

            while (rs.next()) {
                Position position = Position.parse(rs.getString("position"));
                UUID uuid = UUID.fromString(rs.getString("uuid"));

                this.positionCache.put(position, uuid);
            }
        });
    }

    private CompletableFuture<Optional<Locker>> findBy(String column, String value) {
        return this.supplyExecute("SELECT * FROM `lockers` WHERE `" + column + "` = ?;", statement -> {
            statement.setString(1, value);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                Locker locker = new Locker(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("description"),
                    Position.parse(rs.getString("position"))
                );
                
                this.addToCache(locker);
                return Optional.of(locker);
            }
            return Optional.empty();
        });
    }
}
