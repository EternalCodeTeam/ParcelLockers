package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerPageResult;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelLockerDatabaseServiceIntegrationTest extends ParcelLockerIntegrationSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @Test
    void test() {
        File dataFolder = new File("run/plugins/ParcelLockers");
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);

        LockerRepository parcelLockerRepository = new LockerRepositoryOrmLite(databaseManager, new TestScheduler());

        UUID uuid = UUID.randomUUID();
        String description = "Parcel locker description.";
        Position position = new Position(1, 2, 3, "world");


        parcelLockerRepository.save(new Locker(uuid, description, position));

        Optional<Locker> parcelLocker = await(parcelLockerRepository.findByUUID(uuid));
        assertTrue(parcelLocker.isPresent());
        assertEquals(uuid, parcelLocker.get().uuid());

        Optional<Locker> byPosition = await(parcelLockerRepository.findByPosition(position));
        assertTrue(byPosition.isPresent());
        assertEquals(uuid, byPosition.get().uuid());

        LockerPageResult pageResult = await(parcelLockerRepository.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.lockers().size());
        assertEquals(uuid, pageResult.lockers().iterator().next().uuid());

        await(parcelLockerRepository.remove(uuid));
        Optional<Locker> removed = await(parcelLockerRepository.findByUUID(uuid));
        assertTrue(removed.isEmpty());
    }
}
