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


public class ParcelLockerPlaceEvent extends Event implements Cancellable, Listener {


	public ParcelLockerPlaceEvent(final Location loc, final ItemStack item, final Player player) {
		this.item = item;
		this.loc = loc;
		this.player = player;
	}

	private final HandlerList handlers = new HandlerList();
	private final ItemStack parcelLockerItemStackK = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true)
			.make();
	private boolean cancelled;
	private final Location loc;
	private final ItemStack item;
	private final Player player;


	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(final boolean cancel) {
		cancelled = cancel;
	}

	public Location getLocation() {
		return loc;
	}

	public ItemStack getItem() {
		return item;
	}

	public Player getPlayer() {
		return player;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onParcelLockerPlace(final BlockPlaceEvent event) {
		if (event.getItemInHand() == parcelLockerItemStackK) {
			Bukkit.getServer().getPluginManager().callEvent(this);
		}
	}


}
