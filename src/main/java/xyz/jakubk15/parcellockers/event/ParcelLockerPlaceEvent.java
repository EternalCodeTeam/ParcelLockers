package xyz.jakubk15.parcellockers.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
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

	public ParcelLockerPlaceEvent(final Location location, final ItemStack item, final Player player) {
		this.item = item;
		this.location = location;
		this.player = player;
	}

	private final HandlerList handlers = new HandlerList();
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
	public void setCancelled(final boolean cancelled) {
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onParcelLockerPlace(final BlockPlaceEvent event) {
		if (event.getItemInHand() == this.parcelLockerItemStack) {
			Bukkit.getPluginManager().callEvent(this);
		}
	}

}
