package com.eternalcode.parcellockers.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.ParcelLockerDatabaseService;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import panda.utilities.text.Formatter;


public class ParcelLockerBreakController implements Listener {

    private final ParcelLockerDatabaseService parcelLockerDatabaseService;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration.Messages messages;

    public ParcelLockerBreakController(ParcelLockerDatabaseService parcelLockerDatabaseService, NotificationAnnouncer announcer, PluginConfiguration.Messages messages) {
        this.parcelLockerDatabaseService = parcelLockerDatabaseService;
        this.announcer = announcer;
        this.messages = messages;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        Player player = event.getPlayer();
        if (this.parcelLockerDatabaseService.positionCache().containsKey(position)) {
            if (!player.isOp() && !player.hasPermission("parcellockers.admin.break")) {
                event.setCancelled(true);
                this.announcer.sendMessage(player, this.messages.cannotBreakParcelLocker);
                return;
            }
            this.parcelLockerDatabaseService.remove(this.parcelLockerDatabaseService.positionCache().get(position));
            this.announcer.sendMessage(player, this.messages.parcelLockerSuccessfullyDeleted);
            Formatter formatter = new Formatter()
                .register("{X}", position.x())
                .register("{Y}", position.y())
                .register("{Z}", position.z())
                .register("{WORLD}", position.world())
                .register("{PLAYER}", player.getName());
            this.announcer.broadcast(formatter.format(this.messages.broadcastParcelLockerRemoved));
        }

    }
}