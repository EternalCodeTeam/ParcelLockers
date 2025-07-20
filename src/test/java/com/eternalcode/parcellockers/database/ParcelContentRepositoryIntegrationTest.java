package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.ConfigurationManager;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelContentRepositoryIntegrationTest extends IntegrationTestSpec {

    @TempDir
    private Path tempDir;

    private DatabaseManager databaseManager;

    @Test
    void test() {
        File dataFolder = tempDir.resolve("ParcelLockers").toFile();
        PluginConfiguration config = new ConfigurationManager(dataFolder).load(new PluginConfiguration());
        this.databaseManager = new DatabaseManager(config, Logger.getLogger("ParcelLockers"), dataFolder);

        ParcelContentRepository parcelContentRepository = new ParcelContentRepositoryOrmLite(databaseManager, new TestScheduler());

        ParcelContent parcelContent = new ParcelContent(UUID.randomUUID(), List.of(new ItemStack(Material.GOLD_BLOCK, 64)));
        parcelContentRepository.save(parcelContent);
        Optional<ParcelContent> retrievedParcelContent = await(parcelContentRepository.findByUUID(parcelContent.uniqueId()));
        assertTrue(retrievedParcelContent.isPresent(), "ParcelContent should be present");

        parcelContentRepository.remove(parcelContent.uniqueId());
        Optional<ParcelContent> removedParcelContent = await(parcelContentRepository.findByUUID(parcelContent.uniqueId()));
        assertFalse(removedParcelContent.isPresent(), "ParcelContent should be removed");
    }

    @AfterEach
    void tearDown() {
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }

}
