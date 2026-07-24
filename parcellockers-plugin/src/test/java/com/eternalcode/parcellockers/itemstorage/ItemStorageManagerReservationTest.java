package com.eternalcode.parcellockers.itemstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.parcellockers.itemstorage.repository.ItemStorageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

class ItemStorageManagerReservationTest {

    @Test
    void reservedRestoreCannotBeOverwrittenByFreshOrdinaryStorage() {
        ItemStorageRepository repository = mock(ItemStorageRepository.class);
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(repository.fetchAll()).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(repository.delete(org.mockito.ArgumentMatchers.any()))
            .thenReturn(CompletableFuture.completedFuture(1));
        when(repository.save(org.mockito.ArgumentMatchers.any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        ItemStorageManager manager = new ItemStorageManager(repository, server);
        UUID owner = UUID.randomUUID();
        List<ItemStack> original = List.of(mock(ItemStack.class));
        List<ItemStack> fresh = List.of(mock(ItemStack.class));
        ItemStorageReservation reservation = manager.reserve(owner).orElseThrow();

        reservation.delete().join();
        assertThrows(CompletionException.class, () -> manager.create(owner, fresh).join());
        assertThrows(CompletionException.class, () -> manager.delete(owner).join());
        reservation.restore(original).join();

        assertThrows(CompletionException.class, () -> manager.get(owner).join());
        verify(repository).save(new ItemStorage(owner, original));

        reservation.close();
        assertEquals(original, manager.get(owner).join().orElseThrow().items());
        manager.create(owner, fresh).join();

        assertEquals(fresh, manager.get(owner).join().orElseThrow().items());
        verify(repository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }
}
