package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class ParcelFindPageIntegrationTest extends MySqlIntegrationTestSpec {

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void findPageReturnsAllParcelsAcrossSenders() throws SQLException {
        PluginConfig config = this.mysqlConfig();

        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), this.tempDir.toFile());
        databaseManager.connect();
        this.databaseManager = databaseManager;

        ParcelRepository repository = new ParcelRepositoryOrmLite(databaseManager, new TestScheduler());
        for (int i = 0; i < 3; i++) {
            this.await(repository.save(new Parcel(
                UUID.randomUUID(), UUID.randomUUID(), "p" + i, "d", false,
                UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(), ParcelStatus.SENT)));
        }

        PageResult<Parcel> firstPage = this.await(repository.findPage(new Page(0, 2)));
        assertEquals(2, firstPage.items().size());
        assertTrue(firstPage.hasNextPage());

        PageResult<Parcel> secondPage = this.await(repository.findPage(new Page(1, 2)));
        assertEquals(1, secondPage.items().size());
        assertFalse(secondPage.hasNextPage());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
