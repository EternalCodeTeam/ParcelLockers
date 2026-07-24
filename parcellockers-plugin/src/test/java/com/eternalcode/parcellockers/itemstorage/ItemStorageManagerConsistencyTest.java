package com.eternalcode.parcellockers.itemstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import com.eternalcode.multification.notice.NoticeBroadcast;
import com.eternalcode.parcellockers.notification.NoticeService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemStorageManagerConsistencyTest {

    private final ItemStorageRepository repository = mock(ItemStorageRepository.class);
    private final Server server = mock(Server.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final CommandSender commandSender = mock(CommandSender.class);

    @BeforeEach
    void setUp() {
        when(this.server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(this.repository.fetchAll())
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        NoticeBroadcast broadcast = mock(NoticeBroadcast.class, RETURNS_SELF);
        when(this.noticeService.create()).thenReturn(broadcast);
    }

    @Test
    void readStartedBeforeReservationMustCompleteBeforeReservationCanBeAcquired() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Optional<ItemStorage>> fetched = new CompletableFuture<>();
        when(this.repository.fetch(owner)).thenReturn(fetched);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Optional<ItemStorage>> read = manager.get(owner);

        assertTrue(manager.reserve(owner).isEmpty());
        fetched.complete(Optional.empty());
        assertTrue(read.join().isEmpty());
        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void cacheMissGetRejectsSameOwnerDeleteUntilReadCompletes() {
        UUID owner = UUID.randomUUID();
        ItemStorage stale = new ItemStorage(owner, List.of(mock(ItemStack.class)));
        CompletableFuture<Optional<ItemStorage>> fetched = new CompletableFuture<>();
        when(this.repository.fetch(owner)).thenReturn(fetched);
        when(this.repository.delete(owner)).thenReturn(CompletableFuture.completedFuture(1));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Optional<ItemStorage>> read = manager.get(owner);

        assertThrows(CompletionException.class, () -> manager.delete(owner).join());
        verify(this.repository, never()).delete(owner);

        fetched.complete(Optional.of(stale));
        assertEquals(stale, read.join().orElseThrow());
        assertTrue(manager.delete(owner).join());
        verify(this.repository).delete(owner);
    }

    @Test
    void overlappingSameOwnerCreatesAreRejectedBeforeSecondWrite() {
        UUID owner = UUID.randomUUID();
        List<ItemStack> firstItems = List.of(mock(ItemStack.class));
        List<ItemStack> secondItems = List.of(mock(ItemStack.class));
        CompletableFuture<Void> firstSave = new CompletableFuture<>();
        when(this.repository.save(any()))
            .thenReturn(firstSave, CompletableFuture.completedFuture(null));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<ItemStorage> firstCreate = manager.create(owner, firstItems);

        assertThrows(CompletionException.class,
            () -> manager.create(owner, secondItems).join());
        verify(this.repository).save(new ItemStorage(owner, firstItems));

        firstSave.complete(null);
        assertEquals(firstItems, firstCreate.join().items());
        assertEquals(firstItems, manager.get(owner).join().orElseThrow().items());
    }

    @Test
    void differentOwnerCreatesMayOverlap() {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        CompletableFuture<Void> firstSave = new CompletableFuture<>();
        CompletableFuture<Void> secondSave = new CompletableFuture<>();
        when(this.repository.save(any())).thenReturn(firstSave, secondSave);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<ItemStorage> firstCreate = manager.create(firstOwner, List.of());
        CompletableFuture<ItemStorage> secondCreate = manager.create(secondOwner, List.of());

        verify(this.repository).save(new ItemStorage(firstOwner, List.of()));
        verify(this.repository).save(new ItemStorage(secondOwner, List.of()));
        firstSave.complete(null);
        secondSave.complete(null);
        assertEquals(firstOwner, firstCreate.join().owner());
        assertEquals(secondOwner, secondCreate.join().owner());
    }

    @Test
    void getOrCreateMissMustCompleteBeforeReservationCanBeAcquired() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Void> saved = new CompletableFuture<>();
        when(this.repository.fetch(owner))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(this.repository.save(any())).thenReturn(saved);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<ItemStorage> operation =
            manager.getOrCreate(owner, List.of(mock(ItemStack.class)));

        assertTrue(manager.reserve(owner).isEmpty());
        saved.complete(null);
        operation.join();
        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void getOrCreateCacheMissReturnsPersistedStorageWithoutOverwritingIt() {
        UUID owner = UUID.randomUUID();
        ItemStorage persisted = new ItemStorage(owner, List.of(mock(ItemStack.class)));
        when(this.repository.fetch(owner))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(persisted)));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        ItemStorage result = manager.getOrCreate(owner, List.of(mock(ItemStack.class))).join();

        assertEquals(persisted, result);
        verify(this.repository, never()).save(any());
    }

    @Test
    void cacheHitIsRejectedWhileOwnerIsReserved() {
        UUID owner = UUID.randomUUID();
        ItemStorage storage = new ItemStorage(owner, List.of(mock(ItemStack.class)));
        when(this.repository.fetch(owner))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(storage)));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);
        assertEquals(storage, manager.get(owner).join().orElseThrow());
        ItemStorageReservation reservation = manager.reserve(owner).orElseThrow();

        assertThrows(CompletionException.class, () -> manager.get(owner).join());

        reservation.close();
    }

    @Test
    void pendingRepositoryFetchMustDrainBeforeReservedDeleteCanPreventStaleCacheResurrection() {
        UUID owner = UUID.randomUUID();
        ItemStorage stale = new ItemStorage(owner, List.of(mock(ItemStack.class)));
        CompletableFuture<Optional<ItemStorage>> fetched = new CompletableFuture<>();
        when(this.repository.fetch(owner))
            .thenReturn(fetched, CompletableFuture.completedFuture(Optional.empty()));
        when(this.repository.delete(owner)).thenReturn(CompletableFuture.completedFuture(1));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Optional<ItemStorage>> read = manager.get(owner);
        assertTrue(manager.reserve(owner).isEmpty());

        fetched.complete(Optional.of(stale));
        assertEquals(stale, read.join().orElseThrow());
        ItemStorageReservation reservation = manager.reserve(owner).orElseThrow();
        assertTrue(reservation.delete().join());
        reservation.close();

        assertTrue(manager.get(owner).join().isEmpty());
    }

    @Test
    void startupCacheWarmupAndReservationCannotOverlap() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Optional<List<ItemStorage>>> fetchedAll = new CompletableFuture<>();
        when(this.repository.fetchAll()).thenReturn(fetchedAll);

        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        assertTrue(manager.reserve(owner).isEmpty());
        fetchedAll.complete(Optional.empty());
        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void closingReservationDoesNotReleaseOwnershipBeforeRestoreCompletes() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Void> saved = new CompletableFuture<>();
        when(this.repository.save(any())).thenReturn(saved);
        when(this.repository.deleteAll()).thenReturn(CompletableFuture.completedFuture(0));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);
        ItemStorageReservation reservation = manager.reserve(owner).orElseThrow();

        CompletableFuture<ItemStorage> restore =
            reservation.restore(List.of(mock(ItemStack.class)));
        reservation.close();

        assertThrows(CompletionException.class,
            () -> manager.deleteAll(this.commandSender, this.noticeService).join());
        verify(this.repository, never()).deleteAll();

        saved.complete(null);
        restore.join();
        manager.deleteAll(this.commandSender, this.noticeService).join();
        verify(this.repository).deleteAll();
    }

    @Test
    void deleteAllReleasesGlobalOwnershipAfterSuccess() {
        UUID owner = UUID.randomUUID();
        when(this.repository.deleteAll()).thenReturn(CompletableFuture.completedFuture(0));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        manager.deleteAll(this.commandSender, this.noticeService).join();

        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void deleteAllReleasesGlobalOwnershipAfterFailure() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Integer> deleted = new CompletableFuture<>();
        when(this.repository.deleteAll()).thenReturn(deleted);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Void> deleteAll =
            manager.deleteAll(this.commandSender, this.noticeService);
        assertTrue(manager.reserve(owner).isEmpty());

        deleted.completeExceptionally(new IllegalStateException("delete all failed"));
        assertThrows(CompletionException.class, deleteAll::join);
        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void deleteAllReleasesGlobalOwnershipAfterSynchronousRepositoryFailure() {
        UUID owner = UUID.randomUUID();
        when(this.repository.deleteAll())
            .thenThrow(new IllegalStateException("synchronous delete all failure"));
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        assertThrows(CompletionException.class,
            () -> manager.deleteAll(this.commandSender, this.noticeService).join());

        assertTrue(manager.reserve(owner).isPresent());
    }

    @Test
    void deleteAllCannotStartWhileReadIsActive() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Optional<ItemStorage>> fetched = new CompletableFuture<>();
        when(this.repository.fetch(owner)).thenReturn(fetched);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Optional<ItemStorage>> read = manager.get(owner);

        assertThrows(CompletionException.class,
            () -> manager.deleteAll(this.commandSender, this.noticeService).join());
        verify(this.repository, never()).deleteAll();
        fetched.complete(Optional.empty());
        read.join();
    }

    @Test
    void ordinaryOperationsCannotStartDuringGlobalOperation() {
        UUID owner = UUID.randomUUID();
        CompletableFuture<Integer> deleted = new CompletableFuture<>();
        when(this.repository.deleteAll()).thenReturn(deleted);
        ItemStorageManager manager = new ItemStorageManager(this.repository, this.server);

        CompletableFuture<Void> deleteAll =
            manager.deleteAll(this.commandSender, this.noticeService);

        assertThrows(CompletionException.class,
            () -> manager.create(owner, List.of()).join());
        assertThrows(CompletionException.class, () -> manager.delete(owner).join());
        assertThrows(CompletionException.class, () -> manager.get(owner).join());
        assertThrows(CompletionException.class,
            () -> manager.getOrCreate(owner, List.of()).join());
        verify(this.repository, never()).save(any());
        verify(this.repository, never()).delete(owner);
        verify(this.repository, never()).fetch(owner);

        deleted.complete(0);
        assertFalse(deleteAll.isCompletedExceptionally());
        deleteAll.join();
    }
}
