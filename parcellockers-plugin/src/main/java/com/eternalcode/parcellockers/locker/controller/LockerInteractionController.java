package com.eternalcode.parcellockers.locker.controller;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.gui.implementation.locker.LockerGui;
import com.eternalcode.parcellockers.locker.Locker;
import com.eternalcode.parcellockers.locker.LockerManager;
import com.eternalcode.parcellockers.shared.Position;
import com.eternalcode.parcellockers.shared.PositionAdapter;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LockerInteractionController implements Listener {

    private final LockerManager lockerManager;
    private final LockerGui lockerGUI;
    private final Scheduler scheduler;

    public LockerInteractionController(LockerManager lockerManager, LockerGui lockerGUI, Scheduler scheduler) {
        this.lockerManager = lockerManager;
        this.lockerGUI = lockerGUI;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onInventoryOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block.getType() != Material.CHEST) {
            return;
        }

        Position position = PositionAdapter.convert(block.getLocation());

        // Fast path: cancel the interaction synchronously so the vanilla chest never opens.
        Optional<Locker> cached = this.lockerManager.getCached(position);
        if (cached.isPresent()) {
            event.setCancelled(true);
            UUID uuid = cached.get().uuid();
            this.scheduler.run(() -> this.lockerGUI.show(player, uuid));
            return;
        }

        // Slow path: the locker is not cached, so the vanilla chest has already opened by the time
        // the async lookup completes. Close it and open the locker GUI instead (warming the cache).
        this.lockerManager.get(position).thenAccept(optionalLocker -> optionalLocker.ifPresent(locker ->
            this.scheduler.run(() -> {
                player.closeInventory();
                this.lockerGUI.show(player, locker.uuid());
            })));
    }
}
