package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PluginParcelService extends ParcelService {

    CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel);

    CompletableFuture<Boolean> sendWithinParcelOperation(
        Player sender,
        Parcel parcel,
        List<ItemStack> items
    );

    CompletableFuture<Void> rollbackSendWithinParcelOperation(Player sender, Parcel parcel);

    <T> CompletableFuture<T> serializeParcelOperation(
        UUID parcel,
        Supplier<CompletableFuture<T>> operation
    );

    CompletableFuture<Void> updateWithinParcelOperation(Parcel parcel);

    CompletableFuture<Optional<Parcel>> getAuthoritativeWithinParcelOperation(UUID parcel);

    void runParcelOperationCallback(UUID parcel, Runnable callback);

    void invalidate(UUID uuid);

    CompletableFuture<Void> delete(CommandSender sender, Parcel parcel);

    CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService);
}
