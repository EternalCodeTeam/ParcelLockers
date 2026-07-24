package com.eternalcode.parcellockers;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.parcel.service.ParcelDispatchService;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import com.eternalcode.parcellockers.parcel.service.PluginParcelService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        this.scheduler.runAsync(() -> {
            try {
                this.dispatcher.dispatch(sender, parcel, items)
                    .whenComplete((success, throwable) -> {
                        if (throwable != null) {
                            result.completeExceptionally(throwable);
                        } else {
                            result.complete(success);
                        }
                    });
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });
        return result;
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
        return this.delegate.collect(player, parcel);
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
}
