package com.eternalcode.parcellockers.locker.controller;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.multification.shared.Formatter;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.nexo.NexoIntegration;
import com.eternalcode.parcellockers.notification.NoticeService;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

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

        this.lockerManager.get(position).thenAccept((locker) -> {
            if (locker.isEmpty()) {
                return;
            }

            this.scheduler.run(() -> {
                if (!player.hasPermission("parcellockers.admin.break")) {
                    // We assume that the event was already processed and the block is gone,
                    // so we need to restore it manually
                    location.getBlock().setType(block.getType());
                    location.getBlock().setBlockData(blockData);
                    this.noticeService.player(player.getUniqueId(), messages -> messages.locker.cannotBreak);
                    return;
                }

                this.lockerManager.delete(locker.get().uuid(), player.getUniqueId());

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
    public void onBlockDestroy(BlockDestroyEvent event) {
        this.handleDamagedLocker(event.getBlock());
    }

    private void handleDamagedLocker(Block block) {
        BlockData originalData = block.getBlockData();
        Location location = block.getLocation();
        Position position = PositionAdapter.convert(location);
        boolean isNexoBlock = NexoIntegration.isNexoBlock(block);
        String nexoId = isNexoBlock ? NexoIntegration.getNexoId(block) : null;

        this.lockerManager.get(position).thenAccept(locker -> {
            if (locker.isPresent()) {
                this.scheduler.run(() -> {
                    if (isNexoBlock && nexoId != null) {
                        NexoIntegration.placeBlock(location, nexoId);
                    } else {
                        location.getBlock().setType(block.getType());
                        location.getBlock().setBlockData(originalData);
                    }
                });
            }
        });
    }
}
