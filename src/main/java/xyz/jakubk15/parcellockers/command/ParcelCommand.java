package xyz.jakubk15.parcellockers.command;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.menu.ParcelMenu;

import java.util.concurrent.TimeUnit;

public class ParcelCommand extends SimpleCommand {

	private final ParcelLockersPlugin plugin;

	public ParcelCommand(ParcelLockersPlugin plugin) {
		super("parcel|post");
		this.setMinArguments(0);
		this.setUsage("/parcel <send|give|reload> [amount]");
		this.setPermission("parcellockers.command.parcel");
		this.setCooldown(3, TimeUnit.SECONDS);
		this.setPermissionMessage("&cYou don't have permission to use this command. If you believe this is an error, contact the server administration.");
		this.setDescription("Basic parcel command.");
		this.plugin = plugin;
	}

	@Override
	protected void onCommand() {
		this.checkConsole();
		String param = this.args[0];

		if ("send".equals(param)) {
			new ParcelMenu().displayTo(this.getPlayer());

		} else if ("give".equals(param)) {
			this.checkArgs(2, "Please specify a player and an amount of parcel lockers to give.");
			ItemStack parcelLocker = ItemCreator.of(CompMaterial.CHEST, "&aParcel locker")
				.glow(true)
				.amount(Integer.parseInt(this.args[1]))
				.make();

			this.getPlayer().getInventory().addItem(parcelLocker);
			this.tellNoPrefix("&aParcel locker has been successfully added to your inventory.");
		} else if ("reload".equals(param)) {
			this.plugin.reload();
			this.tellNoPrefix("&aParcel lockers have been successfully reloaded.");
		} else {
			this.tellNoPrefix("&cUnknown parameter. Please use /parcel send or /parcel give.");
		}
	}


}
