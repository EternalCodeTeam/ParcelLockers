package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class ParcelContentUpdateIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void updateOverwritesExistingContent() throws SQLException {
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

        ParcelContentRepository repository = new ParcelContentRepositoryOrmLite(databaseManager, new TestScheduler());
        UUID parcel = UUID.randomUUID();
        this.await(repository.save(new ParcelContent(parcel, List.of(new ItemStack(Material.STONE, 1)))));

        this.await(repository.update(new ParcelContent(parcel, List.of(new ItemStack(Material.DIAMOND, 5)))));

        Optional<ParcelContent> reloaded = this.await(repository.find(parcel));
        assertTrue(reloaded.isPresent());
        assertEquals(1, reloaded.get().items().size());
        assertEquals(Material.DIAMOND, reloaded.get().items().getFirst().getType());
        assertEquals(5, reloaded.get().items().getFirst().getAmount());
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
