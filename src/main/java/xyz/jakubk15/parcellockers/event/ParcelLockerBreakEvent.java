package xyz.jakubk15.parcellockers.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;

public class ParcelLockerBreakEvent extends Event implements Cancellable, Listener {

	private boolean cancelled;
	private static final HandlerList handlerList = new HandlerList();
	private final Location loc;
	private final ItemStack item;
	private final ParcelLockersPlugin plugin;
	private final Player player;

	public ParcelLockerBreakEvent(final ParcelLockersPlugin plugin, final Location loc, final ItemStack item, final Player player) {
		this.plugin = plugin;
		this.item = item;
		this.loc = loc;
		this.player = player;
	}


	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Location loc = event.getBlock().getLocation();
		if (event.getBlock() == Material.CHEST.createBlockData() && this.plugin.getParcelDatabase()
			.keySet()
			.stream()
			.anyMatch(parcelLocker ->
				parcelLocker.getLocation()
					.equals(loc))) {
			Bukkit.getPluginManager().callEvent(this);
		}
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	public Location getLocation() {
		return this.loc;
	}

	public ItemStack getItemStack() {
		return this.item;
	}

	public Player getPlayer() {
		return this.player;
	}
}
