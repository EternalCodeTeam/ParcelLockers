package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.eternalcode.parcellockers.TestItemStacks.installDeserializer;
import static com.eternalcode.parcellockers.TestItemStacks.stack;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParcelContentUpdateIntegrationTest extends MySqlIntegrationTestSpec {

    private static final byte ORIGINAL_MARKER = 1;
    private static final byte UPDATED_MARKER = 2;

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void updateOverwritesExistingContent() throws SQLException, ReflectiveOperationException {
        PluginConfig config = this.mysqlConfig();

        DatabaseManager databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), this.tempDir.toFile());
        databaseManager.connect();
        this.databaseManager = databaseManager;

        ParcelContentRepository repository = new ParcelContentRepositoryOrmLite(databaseManager, new TestScheduler());
        ItemStack originalStack = stack(ORIGINAL_MARKER);
        ItemStack updatedStack = stack(UPDATED_MARKER);
        installDeserializer(Map.of(ORIGINAL_MARKER, originalStack, UPDATED_MARKER, updatedStack));

        UUID parcel = UUID.randomUUID();
        this.await(repository.save(new ParcelContent(parcel, List.of(originalStack))));

        this.await(repository.update(new ParcelContent(parcel, List.of(updatedStack))));

        Optional<ParcelContent> reloaded = this.await(repository.find(parcel));
        assertTrue(reloaded.isPresent());
        assertEquals(1, reloaded.get().items().size());
        assertSame(updatedStack, reloaded.get().items().getFirst());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
