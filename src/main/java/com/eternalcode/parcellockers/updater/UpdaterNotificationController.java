package com.eternalcode.parcellockers.updater;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.shared.SentryExceptionHandler;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.CompletableFuture;

public class UpdaterNotificationController implements Listener {

    private static final String NEW_VERSION_AVAILABLE = "<b><gradient:#8a1212:#fc6b03>ParcelLockers:</gradient></b> <color:#fce303>New version of ParcelLockers is available, please update!";

    private final UpdaterService updaterService;
    private final PluginConfiguration pluginConfig;
    private final AudienceProvider audienceProvider;
    private final MiniMessage miniMessage;

    public UpdaterNotificationController(UpdaterService updaterService, PluginConfiguration pluginConfig, AudienceProvider audienceProvider, MiniMessage miniMessage) {
        this.updaterService = updaterService;
        this.pluginConfig = pluginConfig;
        this.audienceProvider = audienceProvider;
        this.miniMessage = miniMessage;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Audience audience = this.audienceProvider.player(player.getUniqueId());

        if (!player.hasPermission("parcellockers.receiveupdates") || !this.pluginConfig.settings.receiveUpdates) {
            return;
        }

        CompletableFuture<Boolean> upToDate = this.updaterService.isUpToDate();

        upToDate.thenAccept(isUpToDate -> {
            if (!isUpToDate) {
                audience.sendMessage(this.miniMessage.deserialize(NEW_VERSION_AVAILABLE));
            }
        }).whenComplete(SentryExceptionHandler.handler());
    }
}
