package com.eternalcode.parcellockers.user;

import com.eternalcode.parcellockers.database.AbstractDatabaseService;
import com.eternalcode.parcellockers.shared.Page;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserRepositoryImpl extends AbstractDatabaseService implements UserRepository {

    private final Map<UUID, User> usersByUUID = new HashMap<>();
    private final Map<String, User> usersByName = new HashMap<>();

    public UserRepositoryImpl(DataSource dataSource) {
        super(dataSource);

        this.initTable();
    }

    private void initTable() {
        this.executeSync("CREATE TABLE IF NOT EXISTS `users`(" +
            "uuid VARCHAR(36) NOT NULL, " +
            "name VARCHAR(16) NOT NULL, " +
            "PRIMARY KEY (uuid)" +
            ");", PreparedStatement::execute);
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(UUID uuid) {
        User user = this.usersByUUID.get(uuid);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.supplyExecute("SELECT * FROM `users` WHERE `uuid` = ?;", statement -> {
            statement.setString(1, uuid.toString());
            return this.extractUser(statement);
        });
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(String name) {
        User user = this.usersByName.get(name);

        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }

        return this.supplyExecute("SELECT * FROM `users` WHERE `name` = ?;", statement -> {
            statement.setString(1, name);
            return this.extractUser(statement);
        });
    }

    @Override
    public CompletableFuture<Void> save(User user) {
        this.usersByUUID.put(user.uuid(), user);
        this.usersByName.put(user.name(), user);

        return this.execute("INSERT INTO `users`(uuid, name) VALUES (?, ?);", statement -> {
            statement.setString(1, user.uuid().toString());
            statement.setString(2, user.name());
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<Void> changeName(UUID uuid, String newName) {
        User user = this.usersByUUID.get(uuid);

        if (user == null) {
            return CompletableFuture.completedFuture(null);
        }

        this.usersByName.remove(user.name());
        user = new User(uuid, newName);
        this.usersByName.put(newName, user);
        this.usersByUUID.put(uuid, user);

        return this.execute("UPDATE `users` SET `name` = ? WHERE `uuid` = ?;", statement -> {
            statement.setString(1, newName);
            statement.setString(2, uuid.toString());
            statement.execute();
        });
    }

    @Override
    public CompletableFuture<UserPageResult> findPage(Page page) {
        return this.supplyExecute("SELECT * FROM `users` LIMIT ?, ?;", statement -> {
            statement.setInt(1, page.getOffset());
            statement.setInt(2, page.getLimit());
            ResultSet resultSet = statement.executeQuery();

            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                User user = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"));
                this.usersByUUID.put(user.uuid(), user);
                this.usersByName.put(user.name(), user);
                users.add(user);
            }

            boolean hasNext = users.size() > page.getLimit();
            if (hasNext) {
                users.remove(users.size() - 1);
            }

            return new UserPageResult(users, hasNext);
        });
    }

    @ApiStatus.Internal
    @NotNull
    private Optional<User> extractUser(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();

        if (!resultSet.next()) {
            return Optional.empty();
        }

        User newUser = new User(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"));
        this.usersByUUID.put(newUser.uuid(), newUser);
        this.usersByName.put(newUser.name(), newUser);

        return Optional.of(newUser);
    }
}
