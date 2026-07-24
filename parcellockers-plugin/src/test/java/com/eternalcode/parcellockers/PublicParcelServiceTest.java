package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicParcelServiceTest {

    @Test
    void sendDefersTheCompleteDispatchWorkflowOffTheCallingThread() {
        PluginParcelService delegate = mock(PluginParcelService.class);
        ParcelDispatchService dispatcher = mock(ParcelDispatchService.class);
        Scheduler scheduler = mock(Scheduler.class);
        PublicParcelService service = new PublicParcelService(delegate, dispatcher, scheduler);
        Player sender = mock(Player.class);
        Parcel parcel = mock(Parcel.class);
        List<ItemStack> items = List.of(mock(ItemStack.class));
        CompletableFuture<Boolean> dispatched = new CompletableFuture<>();
        when(dispatcher.dispatch(sender, parcel, items)).thenReturn(dispatched);

        CompletableFuture<Boolean> result = service.send(sender, parcel, items);

        assertFalse(result.isDone());
        verify(dispatcher, never()).dispatch(sender, parcel, items);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runAsync(task.capture());
        task.getValue().run();

        verify(dispatcher).dispatch(sender, parcel, items);
        dispatched.complete(true);
        assertSame(Boolean.TRUE, result.join());
        verify(delegate, never()).send(sender, parcel, items);
    }

    @Test
    void schedulerSubmissionFailureCompletesFutureExceptionallyWithoutSynchronousThrow() {
        PluginParcelService delegate = mock(PluginParcelService.class);
        ParcelDispatchService dispatcher = mock(ParcelDispatchService.class);
        Scheduler scheduler = mock(Scheduler.class);
        PublicParcelService service = new PublicParcelService(delegate, dispatcher, scheduler);
        Player sender = mock(Player.class);
        Parcel parcel = mock(Parcel.class);
        List<ItemStack> items = List.of(mock(ItemStack.class));
        when(scheduler.runAsync(any()))
            .thenThrow(new IllegalStateException("submission failed"));

        CompletableFuture<Boolean> result = assertDoesNotThrow(() -> service.send(sender, parcel, items));

        assertThrows(CompletionException.class, result::join);
        verify(dispatcher, never()).dispatch(sender, parcel, items);
    }
}
