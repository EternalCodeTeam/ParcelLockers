package com.eternalcode.parcellockers.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eternalcode.parcellockers.TestScheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.content.ParcelContent;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepository;
import com.eternalcode.parcellockers.content.repository.ParcelContentRepositoryOrmLite;
import com.eternalcode.parcellockers.delivery.Delivery;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepository;
import com.eternalcode.parcellockers.delivery.repository.DeliveryRepositoryOrmLite;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.returns.CollectedParcel;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepository;
import com.eternalcode.parcellockers.returns.repository.CollectedParcelRepositoryOrmLite;
import com.eternalcode.parcellockers.returns.repository.ParcelReturnRepository;
import com.eternalcode.parcellockers.returns.repository.ParcelReturnRepositoryOrmLite;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelReturnCommitRepositoryIntegrationTest extends IntegrationTestSpec {

    private DatabaseManager databaseManager;
    private TestScheduler scheduler;
    private ParcelRepository parcelRepository;
    private ParcelContentRepository contentRepository;
    private DeliveryRepository deliveryRepository;
    private CollectedParcelRepository collectedRepository;
    private ParcelReturnRepository returnRepository;

    @BeforeEach
    void setUp() throws SQLException {
        PluginConfig config = new PluginConfig();
        config.settings.databaseType = DatabaseType.H2;

        File dataFolder = new File("build/tmp/parcel-return-" + UUID.randomUUID());
        this.databaseManager = new DatabaseManager(
            config, Logger.getLogger("ParcelLockers"), dataFolder);
        this.databaseManager.connect();
        this.scheduler = new TestScheduler();
        this.parcelRepository = new ParcelRepositoryOrmLite(this.databaseManager, this.scheduler);
        this.contentRepository = new ParcelContentRepositoryOrmLite(this.databaseManager, this.scheduler);
        this.deliveryRepository = new DeliveryRepositoryOrmLite(this.databaseManager, this.scheduler);
        this.collectedRepository = new CollectedParcelRepositoryOrmLite(this.databaseManager, this.scheduler);
        this.returnRepository = new ParcelReturnRepositoryOrmLite(this.databaseManager, this.scheduler);
    }

    @Test
    void concurrentReturnsKeepTheWinningContentAndDelivery() throws ReflectiveOperationException {
        Parcel collected = collectedParcel();
        ItemStack originalStack = stack((byte) 1);
        ItemStack firstStack = stack((byte) 2);
        ItemStack secondStack = stack((byte) 3);

        installDeserializer(Map.of(
            (byte) 1, originalStack,
            (byte) 2, firstStack,
            (byte) 3, secondStack
        ));
        this.await(this.parcelRepository.save(collected));
        this.await(this.contentRepository.save(content(collected.uuid(), originalStack)));
        this.await(this.collectedRepository.save(new CollectedParcel(
            collected.uuid(), Instant.parse("2026-07-14T10:00:00Z"))));

        Parcel returned = returned(collected);
        Instant firstDelivery = Instant.parse("2026-07-14T11:00:00Z");
        Instant secondDelivery = Instant.parse("2026-07-14T12:00:00Z");

        CompletableFuture<Boolean> first = this.returnRepository.commit(
            returned, content(collected.uuid(), firstStack),
            new Delivery(collected.uuid(), firstDelivery));
        CompletableFuture<Boolean> second = this.returnRepository.commit(
            returned, content(collected.uuid(), secondStack),
            new Delivery(collected.uuid(), secondDelivery));

        boolean firstWon = this.await(first);
        boolean secondWon = this.await(second);

        assertNotEquals(firstWon, secondWon);
        ItemStack expectedStack = firstWon ? firstStack : secondStack;
        Instant expectedDelivery = firstWon ? firstDelivery : secondDelivery;
        assertSame(expectedStack, this.await(this.contentRepository.find(collected.uuid()))
            .orElseThrow().items().getFirst());
        assertEquals(expectedDelivery, this.await(this.deliveryRepository.find(collected.uuid()))
            .orElseThrow().deliveryTimestamp());
        assertEquals(ParcelStatus.SENT, this.await(this.parcelRepository.findById(collected.uuid()))
            .orElseThrow().status());
        assertTrue(this.await(this.collectedRepository.find(collected.uuid())).isEmpty());
    }

    @Test
    void failedClaimDoesNotChangeContentOrDelivery() throws ReflectiveOperationException {
        Parcel sent = returned(collectedParcel());
        Instant existingDelivery = Instant.parse("2026-07-14T11:00:00Z");
        ItemStack existingStack = stack((byte) 4);
        ItemStack losingStack = stack((byte) 5);

        installDeserializer(Map.of(
            (byte) 4, existingStack,
            (byte) 5, losingStack
        ));
        this.await(this.parcelRepository.save(sent));
        this.await(this.contentRepository.save(content(sent.uuid(), existingStack)));
        this.await(this.deliveryRepository.save(new Delivery(sent.uuid(), existingDelivery)));

        boolean committed = this.await(this.returnRepository.commit(
            sent, content(sent.uuid(), losingStack),
            new Delivery(sent.uuid(), Instant.parse("2026-07-14T12:00:00Z"))));

        assertFalse(committed);
        assertSame(existingStack, this.await(this.contentRepository.find(sent.uuid()))
            .orElseThrow().items().getFirst());
        assertEquals(existingDelivery, this.await(this.deliveryRepository.find(sent.uuid()))
            .orElseThrow().deliveryTimestamp());
    }

    private static Parcel collectedParcel() {
        return new Parcel(UUID.randomUUID(), UUID.randomUUID(), "parcel", "description", false,
            UUID.randomUUID(), ParcelSize.SMALL, UUID.randomUUID(), UUID.randomUUID(),
            ParcelStatus.COLLECTED);
    }

    private static Parcel returned(Parcel collected) {
        return new Parcel(collected.uuid(), collected.receiver(), collected.name(),
            collected.description(), collected.priority(), collected.sender(), collected.size(),
            collected.destinationLocker(), collected.entryLocker(), ParcelStatus.SENT);
    }

    private static ParcelContent content(UUID parcel, ItemStack item) {
        return new ParcelContent(parcel, List.of(item));
    }

    private static ItemStack stack(byte marker) {
        ItemStack item = mock(ItemStack.class);
        when(item.isEmpty()).thenReturn(false);
        when(item.serializeAsBytes()).thenReturn(new byte[] { marker });
        return item;
    }

    private static void installDeserializer(Map<Byte, ItemStack> items)
        throws ReflectiveOperationException {
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.deserializeItem(any(byte[].class))).thenAnswer(invocation -> {
                byte[] serialized = invocation.getArgument(0);
                return items.get(serialized[0]);
            });
        Server server = mock(Server.class);
        when(server.getUnsafe()).thenReturn(unsafe);
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);
    }

    @AfterEach
    void tearDown() {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }
    }
}
