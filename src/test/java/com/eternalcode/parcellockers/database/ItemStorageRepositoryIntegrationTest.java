package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.itemstorage.ItemStorage;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepositoryOrmLite;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ItemStorageRepositoryIntegrationTest extends IntegrationTestSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = tempDir.resolve("ParcelLockers").toFile();
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        this.databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);
        ItemStorageRepository itemStorageRepository = new ItemStorageRepositoryOrmLite(databaseManager, new TestScheduler());

        ItemStorage itemStorage = new ItemStorage(UUID.randomUUID(), List.of(new ItemStack(Material.GOLD_BLOCK, 64)));
        itemStorageRepository.save(itemStorage);
        Optional<ItemStorage> retrievedItemStorage = await(itemStorageRepository.find(itemStorage.owner()));
        assertTrue(retrievedItemStorage.isPresent(), "ItemStorage should be present");
        assertTrue(retrievedItemStorage.get().items().contains(new ItemStack(Material.GOLD_BLOCK, 64)),
            "ItemStorage should contain the saved item");

        itemStorageRepository.remove(itemStorage.owner());
        Optional<ItemStorage> removedItemStorage = await(itemStorageRepository.find(itemStorage.owner()));
        assertTrue(removedItemStorage.isEmpty(), "ItemStorage should be removed");
    }


    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }

}
