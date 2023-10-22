package com.eternalcode.parcellockers.deliverycode.repository;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.deliverycode.DeliveryCode;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryCodeRepositoryImpl extends AbstractDatabaseService implements DeliveryCodeRepository {

    private final Map<UUID, DeliveryCode> cache = new HashMap<>();

    protected DeliveryCodeRepositoryImpl(DataSource dataSource) {
        super(dataSource);

        this.initTable();
    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `delivery_codes`(" +
            "`parcelUUID` VARCHAR(36) NOT NULL, " +
            "`code` VARCHAR(6) NOT NULL, " +
            "PRIMARY KEY (parcelUUID) " +
            ");", PreparedStatement::execute);
    }

    @Override
    public CompletableFuture<Void> save(DeliveryCode deliveryCode) {
        return this.execute("INSERT INTO `delivery_codes` (`parcelUUID`, `code`) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, deliveryCode.parcelUUID().toString());
            preparedStatement.setString(2, deliveryCode.code());
            preparedStatement.execute();
            this.cache.put(deliveryCode.parcelUUID(), deliveryCode);
        });
    }

    @Override
    public CompletableFuture<Optional<DeliveryCode>> findByUUID(UUID parcelUUID) {
        return this.supplyExecute("SELECT * FROM `delivery_codes` WHERE `parcelUUID` = ?;", statement -> {
            statement.setString(1, parcelUUID.toString());
            ResultSet rs = statement.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            }

           DeliveryCode deliveryCode = new DeliveryCode(UUID.fromString(rs.getString("parcelUUID")), rs.getString("code"));
           
            this.cache.putIfAbsent(parcelUUID, deliveryCode);
           
            return Optional.of(deliveryCode);
        });
    }

    @Override
    public CompletableFuture<Void> remove(DeliveryCode deliveryCode) {
        return this.remove(deliveryCode.parcelUUID());
    }

    @Override
    public CompletableFuture<Void> remove(UUID parcelUUID) {
        return this.supplyExecute("DELETE FROM `delivery_codes` WHERE `parcelUUID` = ?;", statement -> {
            statement.setString(1, parcelUUID.toString());
            statement.execute();
            this.cache.remove(parcelUUID);
            return null;
        });
    }

    public Optional<DeliveryCode> find(UUID parcelUUID) {
       Optional<DeliveryCode> deliveryCodeOptional = Optional.ofNullable(this.cache.get(parcelUUID));
       if (deliveryCodeOptional.isPresent()) {
           return deliveryCodeOptional;    
       }

        return this.findByUUID(parcelUUID).join();
    }
}
