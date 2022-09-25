package xyz.jakubk15.parcellockers.command;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import xyz.jakubk15.parcellockers.menu.ParcelMenu;

import java.util.concurrent.TimeUnit;

public class ParcelCommand extends SimpleCommand {
	public ParcelCommand() {
		super("parcel|post");
		setMinArguments(0);
		setUsage("");
		setPermission("parcellockers.command.parcel");
		setCooldown(3, TimeUnit.SECONDS);
		setPermissionMessage("&cYou don't have permission to use this command. If you believe this is an error, contact the server administration.");
		setDescription("Basic parcel command.");
	}

	@Override
	protected void onCommand() {
		checkConsole();
		final String param = args[0];
		if ("send".equals(param)) {
			new ParcelMenu().displayTo(getPlayer());
		} else if ("give".equals(param)) {
			final ItemStack parcelLocker = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker").glow(true)
					.make();
			getPlayer().getInventory().addItem(parcelLocker);
			tellNoPrefix("&aParcel locker has been successfully added to your inventory.");
		}
	}
}
