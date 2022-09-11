package xyz.jakubk15.parcellockers.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public class ParcelLockerDropEvent extends Event implements Cancellable, Listener {

	private static final HandlerList HANDLERS = new HandlerList();
	private static final ItemStack PARCEL_LOCKER_ITEM_STACK = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true)
			.make();
	private boolean isCancelled;
	private Location loc;
	private ItemStack item;
	private Player player;

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(final boolean cancelled) {
		isCancelled = cancelled;
	}

	public Location getLocation() {
		return loc;
	}

	public ItemStack getItemStack() {
		return item;
	}

	public Player getPlayer() {
		return player;
	}


	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public ParcelLockerDropEvent(final Location loc, final ItemStack item, final Player player) {
		this.item = item;
		this.loc = loc;
		this.player = player;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemDrop(final PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack() == this.PARCEL_LOCKER_ITEM_STACK) {
			Bukkit.getPluginManager().callEvent(this);
		}
	}


}
