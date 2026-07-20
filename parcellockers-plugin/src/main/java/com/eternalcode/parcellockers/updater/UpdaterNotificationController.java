package com.eternalcode.parcellockers.updater;

import com.eternalcode.multification.notice.Notice;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.notification.NoticeService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdaterNotificationController implements Listener {

    private static final String NEW_VERSION_AVAILABLE = "<b><gradient:#8a1212:#fc6b03>ParcelLockers:</gradient></b> <color:#fce303>New version of ParcelLockers is available, please update!";

    private final UpdaterService updaterService;
    private final PluginConfig pluginConfig;
    private final NoticeService noticeService;

    public UpdaterNotificationController(
            UpdaterService updaterService,
            PluginConfig pluginConfig,
            NoticeService noticeService
    ) {
        this.updaterService = updaterService;
        this.pluginConfig = pluginConfig;
        this.noticeService = noticeService;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("parcellockers.receiveupdates") || !this.pluginConfig.settings.receiveUpdates) {
            return;
        }

        CompletableFuture<Boolean> upToDate = this.updaterService.isUpToDate();

        upToDate.thenAccept(isUpToDate -> {
            if (!isUpToDate) {
                this.noticeService.player(player.getUniqueId(), messages -> Notice.chat(NEW_VERSION_AVAILABLE));
            }
        }).orTimeout(5, TimeUnit.SECONDS);
    }
}
