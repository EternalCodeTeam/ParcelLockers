package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelPageResult;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Page;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelDatabaseService extends AbstractDatabaseService implements ParcelRepository {

    private final Map<UUID, Parcel> cache = new HashMap<>();

    public ParcelDatabaseService(DataSource dataSource) {
        super(dataSource);

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
        return this.execute("INSERT INTO `parcels`(uuid, " +
            "`name`, " +
            "`description`, " +
            "`priority`, " +
            "`receiver`, " +
            "`size`, " +
            "`entryLocker`, " +
            "`destinationLocker`, " +
            "`sender`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", statement -> {
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
            
            this.addParcelToCache(parcel);
        });
    }

    @Override
    public CompletableFuture<Void> update(Parcel newParcel) {
        return this.execute("UPDATE `parcels` SET " +
            "`name` = ?, " +
            "`description` = ?, " +
            "`priority` = ?, " +
            "`receiver` = ?, " +
            "`size` = ?, " +
            "`entryLocker` = ?, " +
            "`destinationLocker` = ?, " +
            "`sender` = ? " +
            "WHERE `uuid` = ?", statement -> {
            statement.setString(1, newParcel.name());
            statement.setString(2, newParcel.description());
            statement.setBoolean(3, newParcel.priority());
            statement.setString(4, newParcel.receiver().toString());
            statement.setString(5, newParcel.size().name());
            statement.setString(6, newParcel.entryLocker().toString());
            statement.setString(7, newParcel.destinationLocker().toString());
            statement.setString(8, newParcel.sender().toString());
            statement.setString(9, newParcel.uuid().toString());
            statement.execute();
            
            this.addParcelToCache(newParcel);
        });
    }

    @Override
    public CompletableFuture<Optional<Parcel>> findByUUID(UUID uuid) {
        return this.supplyExecute("SELECT * FROM `parcels` WHERE `uuid` = ?", statement -> {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                Parcel parcel = this.createParcel(rs);
                
                this.addParcelToCache(parcel);
                
                return Optional.of(parcel);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<Parcel>> findBySender(UUID sender) {
        return this.supplyExecute("SELECT * FROM `parcels` WHERE `sender` = ?", statement -> {
            statement.setString(1, sender.toString());
            ResultSet rs = statement.executeQuery();
            
            List<Parcel> parcels = new ArrayList<>();
            while (rs.next()) {
                Parcel parcel = this.createParcel(rs);
                
                this.addParcelToCache(parcel);
                parcels.add(parcel);
            }
            return parcels;
        });
    }

    @Override
    public CompletableFuture<List<Parcel>> findByReceiver(UUID receiver) {
        return this.supplyExecute("SELECT * FROM `parcels` WHERE `receiver` = ?", statement -> {
            statement.setString(1, receiver.toString());
            ResultSet rs = statement.executeQuery();
            
            List<Parcel> parcels = new ArrayList<>();
            while (rs.next()) {
                Parcel parcel = this.createParcel(rs);
                this.addParcelToCache(parcel);
                parcels.add(parcel);
            }
            return parcels;
        });
    }

    @Override
    public CompletableFuture<Void> remove(Parcel parcel) {
        return this.remove(parcel.uuid());
    }

    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return this.supplyExecute("DELETE FROM `parcels` WHERE `uuid` = ?", statement -> {
            statement.setString(1, uuid.toString());
            statement.execute();
            this.removeParcelFromCache(uuid);
            return null;
        });
    }

    @Override
    public CompletableFuture<ParcelPageResult> findPage(Page page) {
        return this.supplyExecute("SELECT * FROM `parcels` LIMIT ? OFFSET ?;", statement -> {
            statement.setInt(1, page.getLimit() + 1);
            statement.setInt(2, page.getOffset());
            ResultSet rs = statement.executeQuery();
            List<Parcel> parcels = new ArrayList<>();
            while (rs.next()) {
                Parcel parcel = this.createParcel(rs);
                this.addParcelToCache(parcel);
                parcels.add(parcel);
            }

            boolean hasNext = parcels.size() > page.getLimit();
            if (hasNext) {
                parcels.remove(parcels.size() - 1);
            }
            return new ParcelPageResult(parcels, hasNext);
        });
    }

    private Parcel createParcel(ResultSet rs) throws SQLException {
        return new Parcel(
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
    }

    private CompletableFuture<Optional<Parcel>> findBy(String column, String value) {
        return this.supplyExecute("SELECT * FROM `parcels` WHERE `" + column + "` = ?;", statement -> {
            statement.setString(1, value);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                Parcel parcel = this.createParcel(rs);
                this.addParcelToCache(parcel);
                return Optional.of(parcel);
            }
            return Optional.empty();
        });
    }


    public Optional<Parcel> findParcel(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    private void addParcelToCache(Parcel parcel) {
        this.cache.put(parcel.uuid(), parcel);
    }

    private void removeParcelFromCache(UUID uuid) {
        this.cache.remove(uuid);
    }

    public Map<UUID, Parcel> cache() {
        return Collections.unmodifiableMap(this.cache);
    }
}
