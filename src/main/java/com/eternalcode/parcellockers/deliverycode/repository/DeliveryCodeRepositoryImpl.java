package com.eternalcode.parcellockers.deliverycode.repository;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.deliverycode.DeliveryCode;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryCodeRepositoryImpl extends AbstractDatabaseService implements DeliveryCodeRepository {

    protected DeliveryCodeRepositoryImpl(DataSource dataSource) {
        super(dataSource);
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
        return null;
    }

    @Override
    public CompletableFuture<DeliveryCode> findByUUID(UUID parcelUUID) {
        return null;
    }

    @Override
    public CompletableFuture<Void> remove(DeliveryCode deliveryCode) {
        return null;
    }

    @Override
    public CompletableFuture<Void> remove(UUID parcelUUID) {
        return null;
    }
}
