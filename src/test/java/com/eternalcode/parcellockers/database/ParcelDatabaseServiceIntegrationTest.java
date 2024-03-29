package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.repository.ParcelPageResult;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepositoryImpl;
import com.eternalcode.parcellockers.shared.Page;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ParcelDatabaseServiceIntegrationTest extends ParcelLockerIntegrationSpec {

    @Container
    private static final MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:latest"));

    @Test
    void test() {
        HikariDataSource dataSource = buildHikariDataSource(mySQLContainer);
        ParcelRepositoryImpl parcelRepositoryImpl = new ParcelRepositoryImpl(dataSource);
        UUID uuid = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID entryLocker = UUID.randomUUID();
        UUID destinationLocker = UUID.randomUUID();

        parcelRepositoryImpl.save(Parcel.builder()
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

        Optional<Parcel> parcel = await(parcelRepositoryImpl.findByUUID(uuid));
        assertTrue(parcel.isPresent());
        assertEquals(uuid, parcel.get().uuid());

        List<Parcel> byReceiver = await(parcelRepositoryImpl.findByReceiver(receiver));
        assertEquals(1, byReceiver.size());
        assertEquals(uuid, byReceiver.iterator().next().uuid());

        List<Parcel> bySender = await(parcelRepositoryImpl.findBySender(sender));
        assertEquals(1, bySender.size());
        assertEquals(uuid, bySender.iterator().next().uuid());

        ParcelPageResult pageResult = await(parcelRepositoryImpl.findPage(new Page(0, 28)));
        assertEquals(1, pageResult.parcels().size());
        assertEquals(uuid, pageResult.parcels().iterator().next().uuid());

        await(parcelRepositoryImpl.remove(uuid));
        Optional<Parcel> removedParcel = await(parcelRepositoryImpl.findByUUID(uuid));
        assertTrue(removedParcel.isEmpty());
    }
}