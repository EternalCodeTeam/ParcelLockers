package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.locker.repository.LockerCache;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.notification.NotificationAnnouncer;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import panda.utilities.text.Formatter;


public class LockerBreakController implements Listener {

    private final LockerRepository lockerRepository;
    private final LockerCache cache;
    private final NotificationAnnouncer announcer;
    private final PluginConfiguration.Messages messages;

    public LockerBreakController(
            LockerRepository lockerRepository,
            LockerCache cache,
            NotificationAnnouncer announcer,
            PluginConfiguration.Messages messages
    ) {
        this.lockerRepository = lockerRepository;
        this.cache = cache;
        this.announcer = announcer;
        this.messages = messages;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        Player player = event.getPlayer();

        if (this.cache.get(position).isEmpty()) {
            return;
        }

        if (!player.hasPermission("parcellockers.admin.break")) {
            event.setCancelled(true);
            this.announcer.sendMessage(player, this.messages.cannotBreakParcelLocker);
            return;
        }

        this.lockerRepository.find(position).thenAccept((locker) -> {
            if (locker.isEmpty()) {
                return;
            }

            UUID toRemove = this.cache.get(position).get().uuid();
            this.lockerRepository.delete(toRemove);

            this.announcer.sendMessage(player, this.messages.parcelLockerSuccessfullyDeleted);

            Formatter formatter = new Formatter()
                    .register("{X}", position.x())
                    .register("{Y}", position.y())
                    .register("{Z}", position.z())
                    .register("{WORLD}", position.world())
                    .register("{PLAYER}", player.getName());

            this.announcer.broadcast(formatter.format(this.messages.broadcastParcelLockerRemoved));
        });
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);

        if (this.cache.get(position).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);

        if (this.cache.get(position).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Location location = block.getLocation();
            Position position = PositionAdapter.convert(location);
            return this.cache.get(position).isPresent();
        });
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);

        if (this.cache.get(position).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);

        if (this.cache.get(position).isPresent()) {
            event.setCancelled(true);
        }
    }
}
