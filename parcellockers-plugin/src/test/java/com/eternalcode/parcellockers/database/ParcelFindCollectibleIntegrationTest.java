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
class ParcelFindCollectibleIntegrationTest extends IntegrationTestSpec {

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

    private void save(ParcelRepository repository, UUID receiver, UUID destinationLocker, ParcelStatus status) {
        this.await(repository.save(new Parcel(
            UUID.randomUUID(), UUID.randomUUID(), "p", "d", false,
            receiver, ParcelSize.SMALL, UUID.randomUUID(), destinationLocker, status)));
    }

    @Test
    void findCollectibleReturnsOnlyDeliveredParcelsForTheGivenLockerAndReceiver() throws SQLException {
        ParcelRepository repository = this.repository();

        UUID receiver = UUID.randomUUID();
        UUID locker = UUID.randomUUID();
        UUID otherLocker = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            this.save(repository, receiver, locker, ParcelStatus.DELIVERED);
        }
        this.save(repository, receiver, locker, ParcelStatus.SENT);          // not yet delivered
        this.save(repository, receiver, otherLocker, ParcelStatus.DELIVERED); // different locker
        this.save(repository, UUID.randomUUID(), locker, ParcelStatus.DELIVERED); // different receiver

        // Pagination must count only the 3 eligible parcels, not the raw receiver page.
        PageResult<Parcel> firstPage = this.await(repository.findCollectible(receiver, locker, new Page(0, 2)));
        assertEquals(2, firstPage.items().size());
        assertTrue(firstPage.hasNextPage());

        PageResult<Parcel> secondPage = this.await(repository.findCollectible(receiver, locker, new Page(1, 2)));
        assertEquals(1, secondPage.items().size());
        assertFalse(secondPage.hasNextPage());
    }

    @Test
    void findCollectibleWithNullLockerReturnsDeliveredParcelsFromAnyLocker() throws SQLException {
        ParcelRepository repository = this.repository();

        UUID receiver = UUID.randomUUID();

        this.save(repository, receiver, UUID.randomUUID(), ParcelStatus.DELIVERED);
        this.save(repository, receiver, UUID.randomUUID(), ParcelStatus.DELIVERED);
        this.save(repository, receiver, UUID.randomUUID(), ParcelStatus.SENT);     // excluded by status
        this.save(repository, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.DELIVERED); // other receiver

        PageResult<Parcel> page = this.await(repository.findCollectible(receiver, null, new Page(0, 10)));
        assertEquals(2, page.items().size());
        assertFalse(page.hasNextPage());
        assertTrue(page.items().stream().allMatch(parcel -> parcel.receiver().equals(receiver)));
        assertTrue(page.items().stream().allMatch(parcel -> parcel.status() == ParcelStatus.DELIVERED));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
