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
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParcelReturnRepositoryIntegrationTest extends MySqlIntegrationTestSpec {

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    private ParcelRepository repository() throws SQLException {
        PluginConfig config = this.mysqlConfig();

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
    void commitCollectionFlipsOnlyDeliveredParcelsOfReceiver() throws SQLException {
        ParcelRepository repository = this.repository();
        new CollectedParcelRepositoryOrmLite(this.databaseManager, new TestScheduler());
        UUID receiver = UUID.randomUUID();
        Parcel delivered = parcel(receiver, UUID.randomUUID(), ParcelStatus.DELIVERED);
        this.await(repository.save(delivered));

        // A player who is not the receiver cannot collect the parcel.
        assertFalse(this.await(repository.commitCollection(delivered.uuid(), UUID.randomUUID(), Instant.now())));

        assertTrue(this.await(repository.commitCollection(delivered.uuid(), receiver, Instant.now())));
        assertEquals(ParcelStatus.COLLECTED, this.await(repository.findById(delivered.uuid())).orElseThrow().status());

        // Second collect attempt must not report success — this is the double-collect guard.
        assertFalse(this.await(repository.commitCollection(delivered.uuid(), receiver, Instant.now())));

        Parcel sent = parcel(receiver, UUID.randomUUID(), ParcelStatus.SENT);
        this.await(repository.save(sent));
        assertFalse(this.await(repository.commitCollection(sent.uuid(), receiver, Instant.now())));
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
