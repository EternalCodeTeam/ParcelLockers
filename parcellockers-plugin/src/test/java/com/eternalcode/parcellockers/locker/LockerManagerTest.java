package com.eternalcode.parcellockers.locker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.locker.validation.LockerValidationService;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import com.eternalcode.parcellockers.shared.validation.ValidationResult;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    @Test
    void rejectsBlankRenameAsAnExceptionalFutureBeforePersistence() {
        LockerRepository repository = mock(LockerRepository.class);
        LockerManager manager = manager(repository);
        UUID lockerId = UUID.randomUUID();

        CompletableFuture<Locker> future = assertDoesNotThrow(() -> manager.rename(lockerId, "  "));
        CompletionException exception = assertThrows(CompletionException.class, future::join);

        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(repository, never()).find(lockerId);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsMissingRenameTargetWithApiValidationException() {
        LockerRepository repository = mock(LockerRepository.class);
        LockerManager manager = manager(repository);
        UUID lockerId = UUID.randomUUID();
        when(repository.find(lockerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        CompletableFuture<Locker> future = assertDoesNotThrow(() -> manager.rename(lockerId, "renamed"));
        CompletionException exception = assertThrows(CompletionException.class, future::join);

        assertInstanceOf(ValidationException.class, exception.getCause());
    }

    @Test
    void successfulRenamePersistsAndRefreshesBothCaches() {
        LockerRepository repository = mock(LockerRepository.class);
        LockerManager manager = manager(repository);
        UUID lockerId = UUID.randomUUID();
        Position position = new Position(1, 2, 3, "world");
        Locker existing = new Locker(lockerId, "old", position);
        Locker renamed = new Locker(lockerId, "new", position);
        when(repository.find(lockerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        when(repository.update(renamed)).thenReturn(CompletableFuture.completedFuture(renamed));

        assertEquals(renamed, manager.rename(lockerId, "new").join());
        assertEquals(Optional.of(renamed), manager.get(lockerId).join());
        assertEquals(Optional.of(renamed), manager.get(position).join());

        verify(repository).update(renamed);
        verify(repository).find(lockerId);
        verify(repository, never()).find(position);
    }

    private static LockerManager manager(LockerRepository repository) {
        return new LockerManager(
            null,
            repository,
            mock(LockerValidationService.class),
            mock(ParcelRepository.class),
            mock(Server.class),
            mock(Scheduler.class)
        );
    }
}
