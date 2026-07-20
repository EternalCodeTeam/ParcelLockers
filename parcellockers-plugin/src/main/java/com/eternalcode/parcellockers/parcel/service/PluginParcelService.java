package com.eternalcode.parcellockers.parcel.service;

import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.parcel.Parcel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface PluginParcelService extends ParcelService {

    CompletableFuture<Void> rollbackSend(Player sender, Parcel parcel);

    void invalidate(UUID uuid);

    CompletableFuture<Void> delete(CommandSender sender, Parcel parcel);

    CompletableFuture<Void> deleteAll(CommandSender sender, NoticeService noticeService);
}
