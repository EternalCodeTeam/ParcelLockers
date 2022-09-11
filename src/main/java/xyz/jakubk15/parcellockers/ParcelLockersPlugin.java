package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.jakubk15.parcellockers.command.ParcelCommand;
import xyz.jakubk15.parcellockers.event.ParcelLockerDropEvent;

public class ParcelLockersPlugin extends SimplePlugin {

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		registerCommand(new ParcelCommand());
		registerEvents(new ParcelLockerDropEvent());
	}

	@Override
	protected void onReloadablesStart() {

	}

	@Override
	public int getFoundedYear() {
		return 2022;
	}

	public static ParcelLockersPlugin getInstance() {
		return (ParcelLockersPlugin) SimplePlugin.getInstance();
	}
}
