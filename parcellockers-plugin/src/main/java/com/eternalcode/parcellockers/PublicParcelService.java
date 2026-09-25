package com.eternalcode.parcellockers;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import com.eternalcode.parcellockers.shared.exception.ValidationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The {@link ParcelService} handed out to other plugins. It validates caller input at the API
 * boundary and runs each mutation on the thread its Bukkit event requires.
 */
final class PublicParcelService implements ParcelService {

    private final PluginParcelService delegate;
    private final ParcelDispatchService dispatcher;
    private final Executor mainThread;
    private final Executor asyncThread;

    PublicParcelService(PluginParcelService delegate, ParcelDispatchService dispatcher, Scheduler scheduler) {
        this.delegate = delegate;
        this.dispatcher = dispatcher;
        this.mainThread = scheduler::run;
        this.asyncThread = scheduler::runAsync;
    }

    @Override
    public CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items) {
        if (!sender.getUniqueId().equals(parcel.sender())) {
            return CompletableFuture.failedFuture(new ValidationException("Sender does not own the parcel"));
        }
        if (parcel.status() != ParcelStatus.SENT) {
            return CompletableFuture.failedFuture(new ValidationException("Parcel must have SENT status"));
        }
        if (items.isEmpty() || items.stream().anyMatch(Objects::isNull)) {
            return CompletableFuture.failedFuture(new ValidationException("Items cannot be empty or contain null"));
        }

        // Snapshot the items now, so later changes by the caller do not leak into the parcel.
        List<ItemStack> snapshot = items.stream()
            .map(ItemStack::clone)
            .toList();
        // ParcelSendEvent is asynchronous, so the whole dispatch runs off the main thread.
        return submit(this.asyncThread, () -> this.dispatcher.dispatch(sender, parcel, snapshot));
    }

    @Override
    public CompletableFuture<Void> collect(Player player, Parcel parcel) {
        if (!player.getUniqueId().equals(parcel.receiver())) {
            return CompletableFuture.failedFuture(new ValidationException("Player is not the parcel receiver"));
        }
        if (parcel.status() != ParcelStatus.DELIVERED) {
            return CompletableFuture.failedFuture(new ValidationException("Parcel must have DELIVERED status"));
        }
        // ParcelCollectEvent is synchronous, so the collection starts on the main thread.
        return submit(this.mainThread, () -> this.delegate.collect(player, parcel));
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

    private static <T> CompletableFuture<T> submit(Executor executor, Supplier<CompletableFuture<T>> operation) {
        return CompletableFuture.supplyAsync(operation, executor).thenCompose(Function.identity());
    }
}
