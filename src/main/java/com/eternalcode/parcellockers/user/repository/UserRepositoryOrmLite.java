package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.user.User;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UserRepositoryOrmLite extends AbstractRepositoryOrmLite implements UserRepository {

    public UserRepositoryOrmLite(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);

        try {
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), UserTable.class);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Optional<User>> fetch(UUID uuid) {
        return this.selectSafe(UserTable.class, uuid).thenApply(optional -> optional
            .map(UserTable::toUser)
        );
    }

    @Override
    public CompletableFuture<Optional<User>> fetch(String name) {
        return this.action(
            UserTable.class, dao -> {
            UserTable userTable = dao.queryForEq("username", name).stream().findFirst().orElse(null);
            return Optional.ofNullable(userTable).map(UserTable::toUser);
        });
    }

    @Override
    public CompletableFuture<Void> save(User user) {
        return this.save(UserTable.class, UserTable.from(user)).exceptionally(ex -> {
            System.err.println("Failed to save user: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> changeName(UUID uuid, String newName) {
        return this.action(
            UserTable.class, dao -> {
            UserTable userTable = dao.queryForId(uuid);
            userTable.setUsername(newName);
            dao.update(userTable);
            return null;
        });
    }

    @Override
    public CompletableFuture<PageResult<User>> fetchPage(Page page) {
        return this.action(
            UserTable.class, dao -> {
            List<User> users = dao.queryBuilder()
                .offset((long) page.getOffset())
                .limit((long) page.getLimit())
                .query()
                .stream().map(UserTable::toUser)
                .collect(Collectors.toList());

            boolean hasNext = users.size() > page.getLimit();
            if (hasNext) {
                users.removeLast();
            }
            return new PageResult<>(users, hasNext);
        });
    }
}
