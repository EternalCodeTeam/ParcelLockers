package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class LockerRenameIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void updateOverwritesExistingName() throws SQLException {
        PluginConfig config = new PluginConfig();
        config.settings.databaseType = DatabaseType.MYSQL;
        config.settings.host = mySQLContainer.getHost();
        config.settings.port = String.valueOf(mySQLContainer.getFirstMappedPort());
        config.settings.databaseName = mySQLContainer.getDatabaseName();
        config.settings.user = mySQLContainer.getUsername();
        config.settings.password = mySQLContainer.getPassword();

        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), this.tempDir.toFile());
        databaseManager.connect();
        this.databaseManager = databaseManager;

        LockerRepository repository = new LockerRepositoryOrmLite(databaseManager, new TestScheduler());
        UUID uuid = UUID.randomUUID();
        Position position = new Position(1, 2, 3, "world");
        this.await(repository.save(new Locker(uuid, "Old name", position)));

        this.await(repository.update(new Locker(uuid, "New name", position)));

        Optional<Locker> reloaded = this.await(repository.find(uuid));
        assertTrue(reloaded.isPresent());
        assertEquals("New name", reloaded.get().name());
        assertEquals(position, reloaded.get().position());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
