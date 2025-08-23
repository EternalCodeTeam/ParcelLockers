package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class LockerBreakController implements Listener {

    private final LockerManager lockerManager;
    private final NoticeService noticeService;

    public LockerBreakController(
            LockerManager lockerManager,
            NoticeService noticeService
    ) {
        this.lockerManager = lockerManager;
        this.noticeService = noticeService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        Player player = event.getPlayer();

        this.lockerManager.get(position).thenAccept((locker) -> {
            if (locker.isEmpty()) {
                return;
            }

            if (!player.hasPermission("parcellockers.admin.break")) {
                event.setCancelled(true);
                this.noticeService.create()
                    .player(player.getUniqueId())
                    .notice(messages -> messages.locker.cannotBreak)
                    .send();
                return;
            }

            this.lockerManager.delete(locker.get().uuid());

            this.noticeService.create()
                .player(player.getUniqueId())
                .notice(messages -> messages.locker.deleted)
                .send();

            Formatter formatter = new Formatter()
                .register("{X}", position.x())
                .register("{Y}", position.y())
                .register("{Z}", position.z())
                .register("{WORLD}", position.world())
                .register("{PLAYER}", player.getName());

            this.noticeService.create()
                .onlinePlayers()
                .notice(messages -> messages.locker.broadcastRemoved)
                .formatter(formatter)
                .send();
        });
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        BlockState explodedBlockState = event.getExplodedBlockState();
        Position position = PositionAdapter.convert(explodedBlockState.getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                event.blockList().remove(explodedBlockState.getBlock());
            }
        });
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Position position = PositionAdapter.convert(block.getLocation());

            this.lockerManager.get(position).thenAccept(locker -> {
                if (locker.isPresent()) {
                    event.blockList().remove(block);
                }
            });
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                event.setCancelled(true);
            }
        });
    }
}
