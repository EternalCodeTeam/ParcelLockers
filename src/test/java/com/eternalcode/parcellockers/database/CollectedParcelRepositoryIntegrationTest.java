package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepositoryOrmLite;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@Testcontainers(disabledWithoutDocker = true)
class CollectedParcelRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    private CollectedParcelRepository repository() throws SQLException {
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

        return new CollectedParcelRepositoryOrmLite(databaseManager, new TestScheduler());
    }

    @Test
    void savesAndFindsCollectedParcel() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        UUID parcel = UUID.randomUUID();
        // MySQL TIMESTAMP columns don't keep nanos; truncate so the round-trip compares equal.
        Instant collectedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        this.await(repository.save(new CollectedParcel(parcel, collectedAt)));

        Optional<CollectedParcel> found = this.await(repository.find(parcel));
        assertTrue(found.isPresent());
        assertEquals(collectedAt, found.get().collectedAt());
    }

    @Test
    void findExpiredReturnsOnlyRowsAtOrBeforeCutoff() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant cutoff = now.minusSeconds(60);

        UUID expired = UUID.randomUUID();
        UUID atCutoff = UUID.randomUUID();
        UUID justAfterCutoff = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        this.await(repository.save(new CollectedParcel(expired, now.minusSeconds(3600))));
        this.await(repository.save(new CollectedParcel(atCutoff, cutoff)));
        // Sub-second offset: guards the temporal (not lexicographic) comparison of the
        // string-persisted column.
        this.await(repository.save(new CollectedParcel(justAfterCutoff, cutoff.plusMillis(500))));
        this.await(repository.save(new CollectedParcel(fresh, now)));

        List<CollectedParcel> result = this.await(repository.findExpired(cutoff));
        List<UUID> resultParcels = result.stream().map(CollectedParcel::parcel).toList();
        assertEquals(2, result.size());
        assertTrue(resultParcels.contains(expired));
        assertTrue(resultParcels.contains(atCutoff));
    }

    @Test
    void deleteRemovesRow() throws SQLException {
        CollectedParcelRepository repository = this.repository();
        UUID parcel = UUID.randomUUID();
        this.await(repository.save(new CollectedParcel(parcel, Instant.now().truncatedTo(ChronoUnit.SECONDS))));

        assertTrue(this.await(repository.delete(parcel)));
        assertFalse(this.await(repository.find(parcel)).isPresent());
        assertFalse(this.await(repository.delete(parcel)));
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
