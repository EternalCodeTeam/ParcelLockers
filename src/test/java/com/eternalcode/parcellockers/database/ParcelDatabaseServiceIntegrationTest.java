package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelPage;
import com.eternalcode.parcellockers.parcel.repository.ParcelPageResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelDatabaseServiceIntegrationTest {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @Test
    void test() {
        HikariDataSource dataSource = buildHikariDataSource();
        ParcelDatabaseService parcelDatabaseService = new ParcelDatabaseService(dataSource);
        UUID uuid = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID entryLocker = UUID.randomUUID();
        UUID destinationLocker = UUID.randomUUID();

        parcelDatabaseService.save(Parcel.builder()
            .uuid(uuid)
            .name("Test")
            .description("Test")
            .priority(false)
            .sender(sender)
            .receiver(receiver)
            .entryLocker(entryLocker)
            .destinationLocker(destinationLocker)
            .size(ParcelSize.SMALL)
            .recipients(Set.of())
            .build()
        );

        Optional<Parcel> parcel = await(parcelDatabaseService.findByUUID(uuid));
        assertTrue(parcel.isPresent());
        assertEquals(uuid, parcel.get().uuid());

        Set<Parcel> byReceiver = await(parcelDatabaseService.findByReceiver(receiver));
        assertEquals(1, byReceiver.size());
        assertEquals(uuid, byReceiver.iterator().next().uuid());

        Set<Parcel> bySender = await(parcelDatabaseService.findBySender(sender));
        assertEquals(1, bySender.size());
        assertEquals(uuid, bySender.iterator().next().uuid());

        ParcelPageResult pageResult = await(parcelDatabaseService.findPage(new ParcelPage(0, 28)));
        assertEquals(1, pageResult.parcels().size());
        assertEquals(uuid, pageResult.parcels().iterator().next().uuid());

        await(parcelDatabaseService.remove(uuid));
        Optional<Parcel> removedParcel = await(parcelDatabaseService.findByUUID(uuid));
        assertTrue(removedParcel.isEmpty());
    }

    private <T> T await(CompletableFuture<T> future) {
        return future
            .orTimeout(5, TimeUnit.SECONDS)
            .join();
    }

    private static HikariDataSource buildHikariDataSource() {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setDriverClassName(mySQLContainer.getDriverClassName());
        hikariConfig.setJdbcUrl(mySQLContainer.getJdbcUrl());
        hikariConfig.setUsername(mySQLContainer.getUsername());
        hikariConfig.setPassword(mySQLContainer.getPassword());

        return new HikariDataSource(hikariConfig);
    }

}