package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicParcelServiceTest {

    @Test
    void sendRejectsSenderMismatchBeforeSchedulingDispatch() {
        Fixture fixture = new Fixture();
        when(fixture.sender.getUniqueId()).thenReturn(UUID.randomUUID());

        CompletableFuture<Boolean> result =
            fixture.service.send(fixture.sender, fixture.parcel, fixture.items);

        assertTrue(result.isCompletedExceptionally());
        CompletionException exception = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(fixture.scheduler, never()).runAsync(any());
        verify(fixture.dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void sendRejectsNonSentParcelBeforeSchedulingDispatch() {
        Fixture fixture = new Fixture();
        Parcel delivered = withStatus(fixture.parcel, ParcelStatus.DELIVERED);

        CompletableFuture<Boolean> result =
            fixture.service.send(fixture.sender, delivered, fixture.items);

        assertTrue(result.isCompletedExceptionally());
        CompletionException exception = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(ValidationException.class, exception.getCause());
        verify(fixture.scheduler, never()).runAsync(any());
        verify(fixture.dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void sendRejectsNullEmptyAndNullElementItemsThroughFailedFuture() {
        Fixture fixture = new Fixture();

        for (List<ItemStack> invalid : List.of(
            List.<ItemStack>of(), Arrays.asList((ItemStack) null))) {
            CompletableFuture<Boolean> result = assertDoesNotThrow(
                () -> fixture.service.send(fixture.sender, fixture.parcel, invalid));
            assertTrue(result.isCompletedExceptionally());
            CompletionException exception = assertThrows(CompletionException.class, result::join);
            assertInstanceOf(ValidationException.class, exception.getCause());
        }
        CompletableFuture<Boolean> nullResult = assertDoesNotThrow(
            () -> fixture.service.send(fixture.sender, fixture.parcel, null));
        assertTrue(nullResult.isCompletedExceptionally());
        assertInstanceOf(ValidationException.class,
            assertThrows(CompletionException.class, nullResult::join).getCause());
        verify(fixture.scheduler, never()).runAsync(any());
    }

    @Test
    void sendFreezesClonedItemsBeforeAsyncSubmission() {
        Fixture fixture = new Fixture();
        ItemStack original = mock(ItemStack.class);
        ItemStack snapshot = mock(ItemStack.class);
        when(original.clone()).thenReturn(snapshot);
        List<ItemStack> callerItems = new ArrayList<>(List.of(original));
        when(fixture.dispatcher.dispatch(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Boolean> result =
            fixture.service.send(fixture.sender, fixture.parcel, callerItems);
        callerItems.clear();

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.scheduler).runAsync(task.capture());
        task.getValue().run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemStack>> capturedItems = ArgumentCaptor.forClass(List.class);
        verify(fixture.dispatcher).dispatch(
            eq(fixture.sender), eq(fixture.parcel), capturedItems.capture());
        assertEquals(List.of(snapshot), capturedItems.getValue());
        assertThrows(UnsupportedOperationException.class,
            () -> capturedItems.getValue().add(mock(ItemStack.class)));
        assertTrue(result.join());
    }

    @Test
    void collectRejectsReceiverMismatchAndInvalidStatusBeforeScheduling() {
        Fixture fixture = new Fixture();
        when(fixture.receiver.getUniqueId()).thenReturn(UUID.randomUUID());

        CompletableFuture<Void> mismatchResult = fixture.service.collect(
            fixture.receiver, withStatus(fixture.parcel, ParcelStatus.DELIVERED));
        assertTrue(mismatchResult.isCompletedExceptionally());
        CompletionException mismatch =
            assertThrows(CompletionException.class, mismatchResult::join);
        assertInstanceOf(ValidationException.class, mismatch.getCause());

        when(fixture.receiver.getUniqueId()).thenReturn(fixture.parcel.receiver());
        CompletableFuture<Void> statusResult =
            fixture.service.collect(fixture.receiver, fixture.parcel);
        assertTrue(statusResult.isCompletedExceptionally());
        CompletionException status =
            assertThrows(CompletionException.class, statusResult::join);
        assertInstanceOf(ValidationException.class, status.getCause());
        verify(fixture.scheduler, never()).runAsync(any());
        verify(fixture.delegate, never()).collect(any(), any());
    }

    @Test
    void collectDefersDelegateWorkOffCallingThread() {
        Fixture fixture = new Fixture();
        Parcel delivered = withStatus(fixture.parcel, ParcelStatus.DELIVERED);
        CompletableFuture<Void> collected = new CompletableFuture<>();
        when(fixture.delegate.collect(fixture.receiver, delivered)).thenReturn(collected);

        CompletableFuture<Void> result = assertDoesNotThrow(
            () -> fixture.service.collect(fixture.receiver, delivered));

        assertFalse(result.isDone());
        verify(fixture.delegate, never()).collect(any(), any());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.scheduler).runAsync(task.capture());
        task.getValue().run();
        verify(fixture.delegate).collect(fixture.receiver, delivered);
        collected.complete(null);
        result.join();
    }

    @Test
    void sendDefersTheCompleteDispatchWorkflowOffTheCallingThread() {
        Fixture fixture = new Fixture();
        CompletableFuture<Boolean> dispatched = new CompletableFuture<>();
        when(fixture.dispatcher.dispatch(
            fixture.sender, fixture.parcel, fixture.items)).thenReturn(dispatched);

        CompletableFuture<Boolean> result =
            fixture.service.send(fixture.sender, fixture.parcel, fixture.items);

        assertFalse(result.isDone());
        verify(fixture.dispatcher, never()).dispatch(any(), any(), any());

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.scheduler).runAsync(task.capture());
        task.getValue().run();

        verify(fixture.dispatcher).dispatch(
            fixture.sender, fixture.parcel, fixture.items);
        dispatched.complete(true);
        assertSame(Boolean.TRUE, result.join());
        verify(fixture.delegate, never()).send(any(), any(), any());
    }

    @Test
    void schedulerSubmissionFailureCompletesFutureExceptionallyWithoutSynchronousThrow() {
        Fixture fixture = new Fixture();
        when(fixture.scheduler.runAsync(any()))
            .thenThrow(new IllegalStateException("submission failed"));

        CompletableFuture<Boolean> result = assertDoesNotThrow(
            () -> fixture.service.send(fixture.sender, fixture.parcel, fixture.items));

        CompletionException exception =
            assertThrows(CompletionException.class, result::join);
        assertInstanceOf(ParcelOperationException.class, exception.getCause());
        assertInstanceOf(IllegalStateException.class, exception.getCause().getCause());
        verify(fixture.dispatcher, never()).dispatch(any(), any(), any());
    }

    private static Parcel withStatus(Parcel parcel, ParcelStatus status) {
        return new Parcel(parcel.uuid(), parcel.sender(), parcel.name(), parcel.description(),
            parcel.priority(), parcel.receiver(), parcel.size(), parcel.entryLocker(),
            parcel.destinationLocker(), status);
    }

    private static final class Fixture {

        private final PluginParcelService delegate = mock(PluginParcelService.class);
        private final ParcelDispatchService dispatcher = mock(ParcelDispatchService.class);
        private final Scheduler scheduler = mock(Scheduler.class);
        private final PublicParcelService service =
            new PublicParcelService(this.delegate, this.dispatcher, this.scheduler);
        private final Player sender = mock(Player.class);
        private final Player receiver = mock(Player.class);
        private final Parcel parcel = new Parcel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "name",
            null,
            false,
            UUID.randomUUID(),
            ParcelSize.SMALL,
            UUID.randomUUID(),
            UUID.randomUUID(),
            ParcelStatus.SENT
        );
        private final List<ItemStack> items = List.of(mock(ItemStack.class));

        private Fixture() {
            when(this.sender.getUniqueId()).thenReturn(this.parcel.sender());
            when(this.receiver.getUniqueId()).thenReturn(this.parcel.receiver());
            when(this.items.get(0).clone()).thenReturn(this.items.get(0));
        }
    }
}
