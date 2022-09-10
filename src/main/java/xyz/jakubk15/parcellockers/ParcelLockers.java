package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.jakubk15.parcellockers.command.ParcelCommand;

public class ParcelLockers extends SimplePlugin {

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		registerCommand(new ParcelCommand());
	}

	@Override
	protected void onReloadablesStart() {

	}

	@Override
	public int getFoundedYear() {
		return 2022;
	}

	public static ParcelLockers getInstance() {
		return (ParcelLockers) SimplePlugin.getInstance();
	}
}
