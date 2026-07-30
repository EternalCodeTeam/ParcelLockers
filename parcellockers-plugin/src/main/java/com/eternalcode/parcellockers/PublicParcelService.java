package com.eternalcode.parcellockers;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ParcelOperationException;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

final class PublicParcelService implements ParcelService {

    private final PluginParcelService delegate;
    private final ParcelDispatchService dispatcher;
    private final Scheduler scheduler;

    PublicParcelService(
        PluginParcelService delegate,
        ParcelDispatchService dispatcher,
        Scheduler scheduler
    ) {
        this.delegate = delegate;
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items) {
        if (this.delegate.isParcelOperationCallbackActive()) {
            return callbackMutationFailure();
        }
        try {
            validateSend(sender, parcel, items);
            List<ItemStack> snapshot = items.stream()
                .map(ItemStack::clone)
                .toList();
            return this.submitAsync(
                () -> this.dispatcher.dispatch(sender, parcel, snapshot),
                "Failed to submit parcel dispatch"
            );
        } catch (ValidationException exception) {
            return CompletableFuture.failedFuture(exception);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(
                operationFailure("Failed to snapshot parcel contents", throwable));
        }
    }

    @Override
    public CompletableFuture<Void> update(Parcel parcel) {
        return this.delegate.update(parcel);
    }

    @Override
    public CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus) {
        return this.delegate.updateIfStatus(updated, expectedStatus);
    }

    @Override
    public CompletableFuture<Void> collect(Player player, Parcel parcel) {
        if (this.delegate.isParcelOperationCallbackActive()) {
            return callbackMutationFailure();
        }
        try {
            validateCollect(player, parcel);
            return this.submitAsync(
                () -> this.delegate.collect(player, parcel),
                "Failed to submit parcel collection"
            );
        } catch (ValidationException exception) {
            return CompletableFuture.failedFuture(exception);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(
                operationFailure("Failed to validate parcel collection", throwable));
        }
    }

    @Override
    public CompletableFuture<Optional<Parcel>> get(UUID uuid) {
        return this.delegate.get(uuid);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page) {
        return this.delegate.getBySender(sender, page);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page) {
        return this.delegate.getByReceiver(receiver, page);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getCollectible(
        UUID receiver,
        @Nullable UUID destinationLocker,
        Page page
    ) {
        return this.delegate.getCollectible(receiver, destinationLocker, page);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page) {
        return this.delegate.getReturnable(receiver, page);
    }

    @Override
    public CompletableFuture<PageResult<Parcel>> getAll(Page page) {
        return this.delegate.getAll(page);
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uuid) {
        return this.delegate.delete(uuid);
    }

    @Override
    public CompletableFuture<Boolean> delete(Parcel parcel) {
        return this.delegate.delete(parcel);
    }

    private <T> CompletableFuture<T> submitAsync(
        Supplier<CompletableFuture<T>> operation,
        String failureMessage
    ) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            this.scheduler.runAsync(() -> {
                try {
                    operation.get().whenComplete((value, throwable) -> {
                        if (throwable != null) {
                            result.completeExceptionally(
                                operationFailure(failureMessage, throwable));
                        } else {
                            result.complete(value);
                        }
                    });
                } catch (Throwable throwable) {
                    result.completeExceptionally(
                        operationFailure(failureMessage, throwable));
                }
            });
        } catch (Throwable throwable) {
            result.completeExceptionally(operationFailure(failureMessage, throwable));
        }
        return result;
    }

    private static void validateSend(Player sender, Parcel parcel, List<ItemStack> items) {
        if (sender == null) {
            throw new ValidationException("Sender cannot be null");
        }
        validateParcel(parcel);
        if (!sender.getUniqueId().equals(parcel.sender())) {
            throw new ValidationException("Sender does not own the parcel");
        }
        if (parcel.status() != ParcelStatus.SENT) {
            throw new ValidationException("Parcel must have SENT status");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Items cannot be null or empty");
        }
        if (items.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("Items cannot contain null elements");
        }
    }

    private static void validateCollect(Player player, Parcel parcel) {
        if (player == null) {
            throw new ValidationException("Player cannot be null");
        }
        validateParcel(parcel);
        if (!player.getUniqueId().equals(parcel.receiver())) {
            throw new ValidationException("Player is not the parcel receiver");
        }
        if (parcel.status() != ParcelStatus.DELIVERED) {
            throw new ValidationException("Parcel must have DELIVERED status");
        }
    }

    private static void validateParcel(Parcel parcel) {
        if (parcel == null) {
            throw new ValidationException("Parcel cannot be null");
        }
        if (parcel.uuid() == null
            || parcel.sender() == null
            || parcel.name() == null
            || parcel.name().isBlank()
            || parcel.receiver() == null
            || parcel.size() == null
            || parcel.entryLocker() == null
            || parcel.destinationLocker() == null
            || parcel.status() == null) {
            throw new ValidationException("Parcel has missing required fields");
        }
    }

    private static ParcelOperationException operationFailure(
        String message,
        Throwable throwable
    ) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ParcelOperationException operationException) {
            return operationException;
        }
        return new ParcelOperationException(message, cause);
    }

    private static <T> CompletableFuture<T> callbackMutationFailure() {
        return CompletableFuture.failedFuture(new IllegalStateException(
            "Parcel mutations cannot be started from a parcel event callback"));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}
