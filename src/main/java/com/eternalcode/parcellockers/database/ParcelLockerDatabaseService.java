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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParcelLockerDatabaseService extends AbstractDatabaseService implements ParcelLockerRepository {

    private final Map<UUID, ParcelLocker> cache = new HashMap<>();
    private final Map<Position, UUID> positionCache = new HashMap<>();

    public ParcelLockerDatabaseService(DataSource dataSource) {
        super(dataSource);

        this.initTable();
    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `parcellockers`(" +
                "uuid VARCHAR(36) NOT NULL, " +
                "description VARCHAR(64) NOT NULL, " +
                "position VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY (uuid)" +
                ");", PreparedStatement::execute);
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
    public CompletableFuture<List<ParcelLocker>> findAll() {
        return this.supplyExecute("SELECT * FROM `parcellockers`;", this::extractParcelLockers);
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
        return this.execute("DELETE FROM `parcellockers` WHERE `uuid` = ?;", statement -> {
            statement.setString(1, uuid.toString());
            statement.execute();
            
            this.removeFromCache(uuid);
        });
    }

    @Override
    public CompletableFuture<Void> remove(ParcelLocker parcelLocker) {
        return this.remove(parcelLocker.uuid());
    }

    @Override
    public CompletableFuture<ParcelLockerPageResult> findPage(Page page) {
        return this.supplyExecute("SELECT * FROM `parcellockers` LIMIT ? OFFSET ?;", statement -> {
            statement.setInt(1, page.getLimit() + 1);
            statement.setInt(2, page.getOffset());
            
            List<ParcelLocker> parcelLockers = this.extractParcelLockers(statement);

            boolean hasNext = parcelLockers.size() > page.getLimit();
            if (hasNext) {
                parcelLockers.remove(parcelLockers.size() - 1);
            }
            return new ParcelLockerPageResult(parcelLockers, hasNext);
        });
    }

    private List<ParcelLocker> extractParcelLockers(PreparedStatement statement) throws SQLException {
        List<ParcelLocker> list = new ArrayList<>();
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            ParcelLocker parcelLocker = new ParcelLocker(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("description"),
                    Position.parse(rs.getString("position"))
            );

            list.add(parcelLocker);
        }

        list.forEach(this::addToCache);
        
        return list;
    }

    public Optional<ParcelLocker> findLocker(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    private void addToCache(ParcelLocker parcelLocker) {
        this.cache.put(parcelLocker.uuid(), parcelLocker);
        this.positionCache.put(parcelLocker.position(), parcelLocker.uuid());
    }

    private void removeFromCache(UUID uuid) {
        this.cache.remove(uuid);
        this.positionCache.remove(this.cache.get(uuid).position());
    }

    public Map<Position, UUID> positionCache() {
        return Collections.unmodifiableMap(this.positionCache);
    }

    public CompletableFuture<Void> updatePositionCache() {
        return this.execute("SELECT * FROM `parcellockers` WHERE `position` IS NOT NULL;", statement -> {
            ResultSet rs = statement.executeQuery();

            this.positionCache.clear();

            while (rs.next()) {
                Position position = Position.parse(rs.getString("position"));
                UUID uuid = UUID.fromString(rs.getString("uuid"));

                this.positionCache.put(position, uuid);
            }
        });
    }

    private CompletableFuture<Optional<ParcelLocker>> findBy(String column, String value) {
        return this.supplyExecute("SELECT * FROM `parcellockers` WHERE `" + column + "` = ?;", statement -> {
            statement.setString(1, value);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                ParcelLocker parcelLocker = new ParcelLocker(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("description"),
                    Position.parse(rs.getString("position"))
                );
                
                this.addToCache(parcelLocker);
                return Optional.of(parcelLocker);
            }
            return Optional.empty();
        });
    }
}
