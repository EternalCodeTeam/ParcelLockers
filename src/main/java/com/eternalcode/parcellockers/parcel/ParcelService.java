package com.eternalcode.parcellockers.parcel;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Page;
import com.eternalcode.parcellockers.shared.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ParcelService {

    void send(Player sender, Parcel parcel, List<ItemStack> items);

    void update(Parcel parcel);

    void collect(Player player, Parcel parcel);

    void delete(CommandSender sender, Parcel parcel);

    void deleteAll(CommandSender sender, NoticeService noticeService);

    CompletableFuture<Optional<Parcel>> get(UUID uuid);

    CompletableFuture<PageResult<Parcel>> getBySender(UUID sender, Page page);

    CompletableFuture<PageResult<Parcel>> getByReceiver(UUID receiver, Page page);

    CompletableFuture<Integer> delete(UUID uuid);

    CompletableFuture<Integer> delete(Parcel parcel);

}
