package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerPage;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerPageResult;
import com.eternalcode.parcellockers.shared.Position;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelLockerDatabaseServiceIntegrationTest {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @Test
    void test() {
        HikariDataSource dataSource = buildHikariDataSource();

        ParcelLockerDatabaseService parcelLockerDatabaseService = new ParcelLockerDatabaseService(dataSource);

        UUID uuid = UUID.randomUUID();
        String description = "Parcel locker description.";
        Position position = new Position(1.0, 2.0, 3.0, 4.0F, 5.0F, "world");


        parcelLockerDatabaseService.save(new ParcelLocker(uuid, description, position));

        Optional<ParcelLocker> parcelLocker = await(parcelLockerDatabaseService.findByUUID(uuid));
        assertTrue(parcelLocker.isPresent());
        assertEquals(uuid, parcelLocker.get().uuid());

        Optional<ParcelLocker> byPosition = await(parcelLockerDatabaseService.findByPosition(position));
        assertTrue(byPosition.isPresent());
        assertEquals(uuid, byPosition.get().uuid());

        ParcelLockerPageResult pageResult = await(parcelLockerDatabaseService.findPage(new ParcelLockerPage(0, 28)));
        assertEquals(1, pageResult.parcelLockers().size());
        assertEquals(uuid, pageResult.parcelLockers().iterator().next().uuid());

        await(parcelLockerDatabaseService.remove(uuid));
        Optional<ParcelLocker> removed = await(parcelLockerDatabaseService.findByUUID(uuid));
        assertTrue(removed.isEmpty());
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
