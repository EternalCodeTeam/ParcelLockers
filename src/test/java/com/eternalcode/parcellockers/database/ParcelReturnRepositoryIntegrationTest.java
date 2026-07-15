package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.nio.file.Path;
import java.sql.SQLException;
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
class ParcelReturnRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    private ParcelRepository repository() throws SQLException {
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

        return new ParcelRepositoryOrmLite(databaseManager, new TestScheduler());
    }

    private static Parcel parcel(UUID receiver, UUID destinationLocker, ParcelStatus status) {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "p", "d", false,
            receiver, ParcelSize.SMALL, UUID.randomUUID(), destinationLocker, status);
    }

    @Test
    void markCollectedFlipsOnlyDeliveredParcels() throws SQLException {
        ParcelRepository repository = this.repository();
        Parcel delivered = parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.DELIVERED);
        this.await(repository.save(delivered));

        assertTrue(this.await(repository.markCollected(delivered.uuid())));
        assertEquals(ParcelStatus.COLLECTED, this.await(repository.findById(delivered.uuid())).orElseThrow().status());

        // Second collect attempt must not report success — this is the double-collect guard.
        assertFalse(this.await(repository.markCollected(delivered.uuid())));

        Parcel sent = parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.SENT);
        this.await(repository.save(sent));
        assertFalse(this.await(repository.markCollected(sent.uuid())));
    }

    @Test
    void findReturnableReturnsOnlyCollectedParcelsOfReceiver() throws SQLException {
        ParcelRepository repository = this.repository();
        UUID receiver = UUID.randomUUID();

        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.COLLECTED)));
        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.COLLECTED)));
        this.await(repository.save(parcel(receiver, UUID.randomUUID(), ParcelStatus.DELIVERED)));
        this.await(repository.save(parcel(UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.COLLECTED)));

        PageResult<Parcel> page = this.await(repository.findReturnable(receiver, new Page(0, 10)));
        assertEquals(2, page.items().size());
        assertTrue(page.items().stream().allMatch(item -> item.status() == ParcelStatus.COLLECTED));
        assertTrue(page.items().stream().allMatch(item -> item.receiver().equals(receiver)));
    }

    @Test
    void collectedParcelsDoNotCountTowardsLockerFullness() throws SQLException {
        ParcelRepository repository = this.repository();
        UUID locker = UUID.randomUUID();

        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.SENT)));
        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.DELIVERED)));
        this.await(repository.save(parcel(UUID.randomUUID(), locker, ParcelStatus.COLLECTED)));

        assertEquals(2, this.await(repository.countParcelsByDestinationLocker(locker)));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
