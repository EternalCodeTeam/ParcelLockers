package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public interface ParcelService {

    CompletableFuture<Boolean> send(Player sender, Parcel parcel, List<ItemStack> items);

    CompletableFuture<Void> update(Parcel parcel);

    CompletableFuture<Boolean> updateIfStatus(Parcel updated, ParcelStatus expectedStatus);

    CompletableFuture<Void> collect(Player player, Parcel parcel);

    CompletableFuture<Optional<Parcel>> get(UUID uuid);

    CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page);

    CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page);

    CompletableFuture<PageResult<Parcel>> getCollectible(
        UUID receiver,
        @Nullable UUID destinationLocker,
        Page page
    );

    CompletableFuture<PageResult<Parcel>> getReturnable(UUID receiver, Page page);

    CompletableFuture<PageResult<Parcel>> getAll(Page page);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Boolean> delete(Parcel parcel);
}
