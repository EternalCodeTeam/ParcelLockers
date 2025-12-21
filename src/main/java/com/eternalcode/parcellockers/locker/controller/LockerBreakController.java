package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import com.spotify.futures.CompletableFutures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        Player player = event.getPlayer();

        this.lockerManager.get(position).thenAccept((locker) -> {
            if (locker.isEmpty()) {
                return;
            }

            this.scheduler.run(() -> {
                if (!player.hasPermission("parcellockers.admin.break")) {
                    // Block was already broken, need to restore it
                    block.getLocation().getBlock().setType(block.getType());
                    this.noticeService.player(player.getUniqueId(), messages -> messages.locker.cannotBreak);
                    return;
                }

                this.lockerManager.delete(locker.get().uuid());

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
            });
        });
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> event.setCancelled(true));
            }
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        BlockState explodedBlockState = event.getExplodedBlockState();
        Position position = PositionAdapter.convert(explodedBlockState.getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> event.blockList().remove(explodedBlockState.getBlock()));
            }
        });
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = new ArrayList<>(event.blockList());

        List<CompletableFuture<Optional<Block>>> futures = blocks.stream()
            .map(block -> {
                Position position = PositionAdapter.convert(block.getLocation());
                return this.lockerManager.get(position)
                    .thenApply(locker -> locker.isPresent() ? Optional.of(block) : Optional.<Block>empty());
            })
            .collect(Collectors.toList());

        CompletableFutures.allAsList(futures)
            .thenAccept(results -> {
                List<Block> blocksToRemove = results.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

                this.scheduler.run(() -> event.blockList().removeAll(blocksToRemove));
            });
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> event.setCancelled(true));
            }
        });
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Position position = PositionAdapter.convert(event.getBlock().getLocation());

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> event.setCancelled(true));
            }
        });
    }
}
