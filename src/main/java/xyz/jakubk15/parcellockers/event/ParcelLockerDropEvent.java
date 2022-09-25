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

	private final HandlerList handlerList = new HandlerList();
	private final ItemStack parcelLockerItemStack = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true)
			.make();
	private boolean isCancelled;
	private final Location loc;
	private final ItemStack item;
	private final Player player;

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
		return handlerList;
	}

	public ParcelLockerDropEvent(final Location loc, final ItemStack item, final Player player) {
		this.item = item;
		this.loc = loc;
		this.player = player;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemDrop(final PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack() == this.parcelLockerItemStack) {
			Bukkit.getServer().getPluginManager().callEvent(this);
		}
	}


}
