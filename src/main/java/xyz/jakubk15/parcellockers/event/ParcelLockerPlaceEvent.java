package xyz.jakubk15.parcellockers.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/*
 * A class representing an event when a
 * parcel locker is placed by a player.
 */

public class ParcelLockerPlaceEvent extends Event implements Cancellable, Listener {

	public ParcelLockerPlaceEvent(Location location, ItemStack item, Player player) {
		this.item = item;
		this.location = location;
		this.player = player;
	}

	private static final HandlerList handlers = new HandlerList();
	private final ItemStack parcelLockerItemStack = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true).make();
	private boolean cancelled;
	private final Location location;
	private final ItemStack item;
	private final Player player;

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public Location getLocation() {
		return this.location;
	}

	public ItemStack getItem() {
		return this.item;
	}

	public Player getPlayer() {
		return this.player;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return this.handlers;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return handlers;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onParcelLockerPlace(BlockPlaceEvent event) {
		if (event.getItemInHand().equals(this.parcelLockerItemStack)) {
			Bukkit.getPluginManager().callEvent(this);
		}
	}

}
