package com.eternalcode.parcellockers.itemstorage.repository;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemStorageRepositoryImpl extends AbstractDatabaseService implements ItemStorageRepository {

    public ItemStorageRepositoryImpl(DataSource dataSource) {
        super(dataSource);

        this.initTable();
    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `item_storage`(`uuid` VARCHAR(36) NOT NULL, `items` TEXT(16000) NOT NULL, PRIMARY KEY (uuid));", PreparedStatement::execute);
    }

    @Override
    public CompletableFuture<Void> save(ItemStorage itemStorage) {
        return this.execute("INSERT INTO `item_storage`(`uuid`, `items`) VALUES (?, ?)", statement -> {
            statement.setString(1, itemStorage.owner().toString());
            statement.setString(2, itemStorage.serializedItemStacks().toString());
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<Optional<ItemStorage>> find(UUID uuid) {
        return this.supplyExecute("SELECT * FROM `item_storage` WHERE `uuid` = ?", statement -> {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(new ItemStorage(
                    UUID.fromString(resultSet.getString("uuid")),
                    this.parseStringList(resultSet)
            ));
        });
    }

    private List<String> parseStringList(ResultSet rs) throws SQLException {
        List<String> result = new ArrayList<>();
        while (rs.next()) {
            String listString = rs.getString("items");

            String[] elements = listString.split(",");
            for (String element : elements) {
                result.add(element.trim());
            }
        }
        return result;
    }

}
