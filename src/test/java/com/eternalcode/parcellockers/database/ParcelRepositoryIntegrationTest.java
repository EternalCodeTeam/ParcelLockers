package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigService;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
class ParcelRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = this.tempDir.resolve("ParcelLockers").toFile();
        PluginConfig config = new ConfigService().create(PluginConfig.class, new File(dataFolder, "config.yml"));
        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);
        this.databaseManager = databaseManager;

        ParcelRepository parcelRepository = new ParcelRepositoryOrmLite(databaseManager, new TestScheduler());
        UUID uuid = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID entryLocker = UUID.randomUUID();
        UUID destinationLocker = UUID.randomUUID();

        parcelRepository.save(new Parcel(uuid, sender, "name", "description", true, receiver,
            ParcelSize.SMALL, entryLocker, destinationLocker, ParcelStatus.SENT));

        Optional<Parcel> parcel = this.await(parcelRepository.findById(uuid));
        assertTrue(parcel.isPresent());
        assertEquals(uuid, parcel.get().uuid());

        List<Parcel> byReceiver = this.await(parcelRepository.findByReceiver(receiver));
        assertEquals(1, byReceiver.size());
        assertEquals(uuid, byReceiver.getFirst().uuid());

        List<Parcel> bySender = this.await(parcelRepository.findBySender(sender));
        assertEquals(1, bySender.size());
        assertEquals(uuid, bySender.getFirst().uuid());

        this.await(parcelRepository.delete(uuid));
        Optional<Parcel> removedParcel = this.await(parcelRepository.findById(uuid));
        assertTrue(removedParcel.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}