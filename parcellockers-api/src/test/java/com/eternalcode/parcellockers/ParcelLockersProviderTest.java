package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParcelLockersProviderTest {

    @AfterEach
    void resetProvider() {
        try {
            ParcelLockersProvider.deinitialize();
        } catch (IllegalStateException ignored) {
            // The test already left the provider empty.
        }
    }

    @Test
    void rejectsAccessBeforeInitialization() {
        assertThrows(IllegalStateException.class, ParcelLockersProvider::provide);
    }

    @Test
    void returnsInitializedApi() {
        ParcelLockersApi api = stubApi();

        ParcelLockersProvider.initialize(api);

        assertSame(api, ParcelLockersProvider.provide());
    }

    @Test
    void rejectsDuplicateInitialization() {
        ParcelLockersProvider.initialize(stubApi());

        assertThrows(IllegalStateException.class,
            () -> ParcelLockersProvider.initialize(stubApi()));
    }

    @Test
    void rejectsAccessAfterDeinitialization() {
        ParcelLockersProvider.initialize(stubApi());

        ParcelLockersProvider.deinitialize();

        assertThrows(IllegalStateException.class, ParcelLockersProvider::provide);
        assertThrows(IllegalStateException.class, ParcelLockersProvider::deinitialize);
    }

    private static ParcelLockersApi stubApi() {
        return new ParcelLockersApi() {
            @Override
            public ParcelService getParcelService() {
                throw new UnsupportedOperationException();
            }

            @Override
            public LockerService getLockerService() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
