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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/*
 * A class representing an event when a player
 * drops a parcel locker as an item.
 */

public class ParcelLockerDropEvent extends Event implements Cancellable, Listener {

	private static final HandlerList handlerList = new HandlerList();
	private final ItemStack parcelLockerItemStack = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true).make();
	private boolean isCancelled;
	private final Location location;
	private final ItemStack item;
	private final Player player;

	public ParcelLockerDropEvent(Location location, ItemStack item, Player player) {
		this.item = item;
		this.location = location;
		this.player = player;
	}

	@Override
	public boolean isCancelled() {
		return this.isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public Location getLocation() {
		return this.location;
	}

	public ItemStack getItemStack() {
		return this.item;
	}

	public Player getPlayer() {
		return this.player;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return handlerList;
	}


	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemDrop(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().equals(this.parcelLockerItemStack)) {
			Bukkit.getPluginManager().callEvent(this);
		}
	}


}
