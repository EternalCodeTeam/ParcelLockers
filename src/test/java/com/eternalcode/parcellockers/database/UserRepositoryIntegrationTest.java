package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.user.User;
import com.eternalcode.parcellockers.user.repository.UserRepository;
import com.eternalcode.parcellockers.user.repository.UserRepositoryOrmLite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer container = new MySQLContainer();

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = tempDir.resolve("ParcelLockers").toFile();
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        DatabaseManager databaseManager = new DatabaseManager(config, null, dataFolder);
        this.databaseManager = databaseManager;

        UserRepository userRepository = new UserRepositoryOrmLite(databaseManager, new TestScheduler());

        UUID userUuid = UUID.randomUUID();
        String username = "testUser";
        User user = new User(userUuid, username);

        userRepository.save(user);

        Optional<User> userOptional = await(userRepository.getUser(userUuid));
        assertTrue(userOptional.isPresent());
        User retrievedUser = userOptional.get();
        assertEquals(retrievedUser.uuid(), userUuid);

        userRepository.getPage(new Page(0, 10))
            .thenAccept(userPageResult -> assertTrue(userPageResult.users().stream().anyMatch(u -> u.uuid().equals(userUuid))));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }

}
