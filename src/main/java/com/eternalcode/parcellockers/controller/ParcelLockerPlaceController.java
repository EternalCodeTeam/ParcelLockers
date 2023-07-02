package com.eternalcode.parcellockers.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.conversation.ParcelLockerPlacePrompt;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.parcellocker.ParcelLocker;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NullConversationPrefix;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class ParcelLockerPlaceController implements Listener {

    private final PluginConfiguration config;
    private final MiniMessage miniMessage;
    private final Plugin plugin;
    private final ParcelLockerDatabaseService databaseService;
    private final NotificationAnnouncer announcer;

    public ParcelLockerPlaceController(PluginConfiguration config, MiniMessage miniMessage, Plugin plugin, ParcelLockerDatabaseService databaseService, NotificationAnnouncer announcer) {
        this.config = config;
        this.miniMessage = miniMessage;
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.announcer = announcer;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        //if (event.getItemInHand().equals(this.config.settings.parcelLockerItem.toGuiItem(this.miniMessage).getItemStack())) {
            ConversationFactory conversationFactory = new ConversationFactory(this.plugin)
                .addConversationAbandonedListener(e -> {
                    if (e.gracefulExit()) {
                        String description = (String) e.getContext().getSessionData("description");
                        Location location = event.getBlockPlaced().getLocation();
                        this.databaseService.save(new ParcelLocker(UUID.randomUUID(), description, PositionAdapter.convert(location))).whenComplete((parcelLocker, throwable) -> {
                            if (throwable != null) {
                                throwable.printStackTrace();
                                this.announcer.sendMessage(player, this.config.messages.failedToCreateParcelLocker);
                            } else {
                                this.announcer.sendMessage(player, this.config.messages.parcelLockerSuccessfullyCreated);
                            }
                        });
                    } else {
                        event.setCancelled(true);
                    }
                })
                .withPrefix(new NullConversationPrefix())
                .withModality(false)
                .withFirstPrompt(new ParcelLockerPlacePrompt(this.announcer, this.config));

            player.beginConversation(conversationFactory.buildConversation(player));
        //}
    }
}
