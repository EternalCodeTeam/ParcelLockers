package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.parcellocker.repository.ParcelLockerPageResult;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.Position;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelLockerDatabaseServiceIntegrationTest extends ParcelLockerIntegrationSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @Test
    void test() {
        HikariDataSource dataSource = buildHikariDataSource(mySQLContainer);

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

        ParcelLockerPageResult pageResult = await(parcelLockerDatabaseService.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.parcelLockers().size());
        assertEquals(uuid, pageResult.parcelLockers().iterator().next().uuid());

        await(parcelLockerDatabaseService.remove(uuid));
        Optional<ParcelLocker> removed = await(parcelLockerDatabaseService.findByUUID(uuid));
        assertTrue(removed.isEmpty());
    }
}
