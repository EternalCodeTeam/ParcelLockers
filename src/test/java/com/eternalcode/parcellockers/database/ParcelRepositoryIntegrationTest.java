package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelCache;
import com.eternalcode.parcellockers.parcel.repository.ParcelPageResult;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = tempDir.resolve("ParcelLockers").toFile();
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);
        this.databaseManager = databaseManager;
        ParcelCache cache = new ParcelCache();

        ParcelRepository parcelRepository = new ParcelRepositoryOrmLite(databaseManager, new TestScheduler(), cache);
        UUID uuid = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID entryLocker = UUID.randomUUID();
        UUID destinationLocker = UUID.randomUUID();

        parcelRepository.save(new Parcel(uuid, sender, "name", "description", true, receiver,
            ParcelSize.SMALL, entryLocker, destinationLocker, ParcelStatus.PENDING));

        Optional<Parcel> parcel = this.await(parcelRepository.findByUUID(uuid));
        assertTrue(parcel.isPresent());
        assertEquals(uuid, parcel.get().uuid());

        List<Parcel> byReceiver = this.await(parcelRepository.findByReceiver(receiver)).orElse(Collections.emptyList());
        assertEquals(1, byReceiver.size());
        assertEquals(uuid, byReceiver.getFirst().uuid());

        List<Parcel> bySender = this.await(parcelRepository.findBySender(sender)).orElse(Collections.emptyList());
        assertEquals(1, bySender.size());
        assertEquals(uuid, bySender.getFirst().uuid());

        ParcelPageResult pageResult = this.await(parcelRepository.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.parcels().size());
        assertEquals(uuid, pageResult.parcels().getFirst().uuid());

        this.await(parcelRepository.remove(uuid));
        Optional<Parcel> removedParcel = this.await(parcelRepository.findByUUID(uuid));
        assertTrue(removedParcel.isEmpty());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}