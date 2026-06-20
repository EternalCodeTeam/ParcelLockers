package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class LockerBreakController implements Listener {

    private final LockerManager lockerManager;
    private final NoticeService noticeService;
    private final Scheduler scheduler;

    public LockerBreakController(
            LockerManager lockerManager,
            NoticeService noticeService, Scheduler scheduler
    ) {
        this.lockerManager = lockerManager;
        this.noticeService = noticeService;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockData blockData = block.getBlockData();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        Player player = event.getPlayer();

        // Fast path: the locker is cached, so we can decide synchronously and cancel the break
        // before vanilla destroys the block and spawns its drops.
        Optional<Locker> cached = this.lockerManager.getCached(position);
        if (cached.isPresent()) {
            event.setCancelled(true);
            this.applyBreakRules(player, position, cached.get(), block);
            return;
        }

        // Slow path: the locker is not cached, so the break has already been processed by the
        // time the async lookup completes. Restore the block to avoid losing the locker.
        this.lockerManager.get(position).thenAccept(locker -> locker.ifPresent(value ->
            this.scheduler.run(() -> {
                location.getBlock().setType(block.getType());
                location.getBlock().setBlockData(blockData);
                this.applyBreakRules(player, position, value, location.getBlock());
            })));
    }

    private void applyBreakRules(Player player, Position position, Locker locker, Block block) {
        if (!player.hasPermission("parcellockers.admin.break")) {
            this.noticeService.player(player.getUniqueId(), messages -> messages.locker.cannotBreak);
            return;
        }

        // Admin removal: delete the managed locker and clear the block without dropping it.
        block.setType(Material.AIR);

        this.lockerManager.delete(locker.uuid(), player.getUniqueId());

        this.noticeService.player(player.getUniqueId(), messages -> messages.locker.deleted);

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
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() != Material.CHEST) {
                continue;
            }
            this.handleDamagedLocker(block);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        this.handleDamagedLocker(event.getBlock());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        this.handleDamagedLocker(event.getBlock());
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        this.handleDamagedLocker(event.getBlock());
    }

    private void handleDamagedLocker(Block block) {
        BlockData blockData = block.getBlockData();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> {
                    location.getBlock().setType(block.getType());
                    location.getBlock().setBlockData(blockData);
                });
            }
        });
    }
}
