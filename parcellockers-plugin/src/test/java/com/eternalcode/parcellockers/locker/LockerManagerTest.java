package com.eternalcode.parcellockers.locker;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.validation.LockerValidationService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;

class LockerManagerTest {

    @Test
    void exposesApiValidationExceptionForInvalidCreateRequest() {
        LockerValidationService validation = mock(LockerValidationService.class);
        UUID lockerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Position position = new Position(1, 2, 3, "world");
        when(validation.validateCreateParameters(lockerId, "locker", position))
            .thenReturn(ValidationResult.invalid("invalid locker"));

        LockerManager manager = new LockerManager(
            null,
            mock(LockerRepository.class),
            validation,
            mock(ParcelRepository.class),
            mock(Server.class),
            mock(Scheduler.class)
        );

        CompletionException exception = assertThrows(CompletionException.class,
            () -> manager.create(lockerId, "locker", position, playerId).join());

        assertInstanceOf(ValidationException.class, exception.getCause());
    }
}
