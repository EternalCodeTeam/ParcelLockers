package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import java.io.File;
import java.nio.file.Path;
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

@Testcontainers
class LockerRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = this.tempDir.resolve("ParcelLockers").toFile();
        PluginConfig config = new ConfigService().create(PluginConfig.class, new File(dataFolder, "config.yml"));
        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);
        this.databaseManager = databaseManager;
        LockerCache cache = new LockerCache();

        LockerRepository parcelLockerRepository = new LockerRepositoryOrmLite(databaseManager, new TestScheduler());

        UUID uuid = UUID.randomUUID();
        String description = "Parcel locker name.";
        Position position = new Position(1, 2, 3, "world");


        parcelLockerRepository.save(new Locker(uuid, description, position));

        Optional<Locker> parcelLocker = this.await(parcelLockerRepository.find(uuid));
        assertTrue(parcelLocker.isPresent());
        assertEquals(uuid, parcelLocker.get().uuid());

        Optional<Locker> byPosition = this.await(parcelLockerRepository.find(position));
        assertTrue(byPosition.isPresent());
        assertEquals(uuid, byPosition.get().uuid());

        LockerPageResult pageResult = this.await(parcelLockerRepository.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.lockers().size());
        assertEquals(uuid, pageResult.lockers().getFirst().uuid());

        this.await(parcelLockerRepository.delete(uuid));
        Optional<Locker> removed = this.await(parcelLockerRepository.find(uuid));
        assertTrue(removed.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
