package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepositoryOrmLite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class DeliveryRepositoryIntegrationTest extends IntegrationTestSpec{

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = tempDir.resolve("ParcelLockers").toFile();
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        config.settings.databaseType = DatabaseType.MYSQL;
        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);
        this.databaseManager = databaseManager;
        DeliveryRepository deliveryRepository = new DeliveryRepositoryOrmLite(databaseManager, new TestScheduler());

        Delivery delivery = new Delivery(UUID.randomUUID(), Instant.now());
        deliveryRepository.save(delivery);

        Optional<Delivery> deliveryOptional = await(deliveryRepository.find(delivery.parcel()));
        assertTrue(deliveryOptional.isPresent(), "Delivery should be present");
        assertEquals(delivery.parcel(), deliveryOptional.get().parcel());

        deliveryRepository.remove(delivery.parcel());
        Optional<Delivery> removedDelivery = await(deliveryRepository.find(delivery.parcel()));
        assertTrue(removedDelivery.isEmpty(), "Delivery should be removed");
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }

}
