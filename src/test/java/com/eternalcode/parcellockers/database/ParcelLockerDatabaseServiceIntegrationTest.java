package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.repository.LockerPageResult;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.repository.LockerRepositoryImpl;
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

        LockerRepository parcelLockerRepository = new LockerRepositoryImpl(dataSource);

        UUID uuid = UUID.randomUUID();
        String description = "Parcel locker description.";
        Position position = new Position(1, 2, 3, "world");


        parcelLockerRepository.save(new Locker(uuid, description, position));

        Optional<Locker> parcelLocker = await(parcelLockerRepository.findByUUID(uuid));
        assertTrue(parcelLocker.isPresent());
        assertEquals(uuid, parcelLocker.get().uuid());

        Optional<Locker> byPosition = await(parcelLockerRepository.findByPosition(position));
        assertTrue(byPosition.isPresent());
        assertEquals(uuid, byPosition.get().uuid());

        LockerPageResult pageResult = await(parcelLockerRepository.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.lockers().size());
        assertEquals(uuid, pageResult.lockers().iterator().next().uuid());

        await(parcelLockerRepository.remove(uuid));
        Optional<Locker> removed = await(parcelLockerRepository.findByUUID(uuid));
        assertTrue(removed.isEmpty());
    }
}
