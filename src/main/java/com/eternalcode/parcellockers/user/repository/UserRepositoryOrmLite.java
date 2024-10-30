package com.eternalcode.parcellockers.user.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.database.DatabaseManager;
import com.eternalcode.parcellockers.database.wrapper.AbstractRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import com.j256.ormlite.table.TableUtils;
import io.sentry.Sentry;

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
            TableUtils.createTableIfNotExists(databaseManager.connectionSource(), UserWrapper.class);
        } catch (SQLException exception) {
            Sentry.captureException(exception);
            exception.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(UUID uuid) {
        return this.select(UserWrapper.class, uuid).thenApply(userWrapper -> Optional.ofNullable(userWrapper.toUser()));
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(String name) {
        return this.action(UserWrapper.class, dao -> {
            UserWrapper userWrapper = dao.queryForEq("username", name).stream().findFirst().orElse(null);
            return Optional.ofNullable(userWrapper).map(UserWrapper::toUser);
        });
    }

    @Override
    public CompletableFuture<Void> save(User user) {
        return this.save(UserWrapper.class, UserWrapper.from(user)).thenApply(dao -> null);
    }

    @Override
    public CompletableFuture<Void> changeName(UUID uuid, String newName) {
        return this.action(UserWrapper.class, dao -> {
            UserWrapper userWrapper = dao.queryForId(uuid);
            userWrapper.setUsername(newName);
            dao.update(userWrapper);
            return null;
        });
    }

    @Override
    public CompletableFuture<UserPageResult> getPage(Page page) {
        return this.action(UserWrapper.class, dao -> {
            List<User> users = dao.queryBuilder()
                .offset((long) page.getOffset())
                .limit((long) page.getLimit())
                .query()
                .stream().map(UserWrapper::toUser)
                .collect(Collectors.toList());

            boolean hasNext = users.size() > page.getLimit();
            if (hasNext) {
                users.remove(users.size() - 1);
            }
            return new UserPageResult(users, hasNext);
        });
    }
}
