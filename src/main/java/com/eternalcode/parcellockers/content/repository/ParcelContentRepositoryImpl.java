package com.eternalcode.parcellockers.content.repository;

import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.util.ItemUtil;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParcelContentRepositoryImpl extends AbstractDatabaseService implements ParcelContentRepository {

    public ParcelContentRepositoryImpl(DataSource dataSource) {
        super(dataSource);

        this.initTable();

    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `parcel_content`(`uuid` VARCHAR(36) NOT NULL, `items` BLOB NULL, PRIMARY KEY (uuid));", PreparedStatement::execute);
    }

    @Override
    public CompletableFuture<Void> save(ParcelContent parcelContent) {
        return this.execute("INSERT INTO `parcel_content`(`uuid`, `items`) VALUES (?, ?)", statement -> {
            statement.setString(1, parcelContent.uniqueId().toString());
            statement.setString(2, ItemUtil.serializeItems(parcelContent.items()));
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<Void> remove(UUID uniqueId) {
        return this.execute("DELETE FROM `parcel_content` WHERE `uuid` = ?", statement -> {
            statement.setString(1, uniqueId.toString());
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<Void> update(ParcelContent parcelContent) {
        return this.execute("UPDATE `parcel_content` SET `items` = ? WHERE `uuid` = ?", statement -> {
            statement.setString(1, ItemUtil.serializeItems(parcelContent.items()));
            statement.setString(2, parcelContent.uniqueId().toString());
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<Optional<ParcelContent>> find(UUID uniqueId) {
        return this.supplyExecute("SELECT * FROM `parcel_content` WHERE `uuid` = ?", statement -> {
            statement.setString(1, uniqueId.toString());
            ResultSet resultSet = statement.executeQuery();
            return Optional.of(new ParcelContent(uniqueId, ItemUtil.deserializeItems(resultSet.getString("items"))));
        });
    }

}
