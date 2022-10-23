package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.jakubk15.parcellockers.command.ParcelCommand;
import xyz.jakubk15.parcellockers.listener.ParcelLockerDropListener;
import xyz.jakubk15.parcellockers.model.Parcel;

import java.util.HashSet;
import java.util.Set;


/*
 * A main plugin class
 */


public class ParcelLockersPlugin extends SimplePlugin {

	// Temporary database, will be removed in next releases.
	public Set<Parcel> parcelSet = new HashSet<>();

	/*
	 * Plugin start method
	 */

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		registerCommand(new ParcelCommand());
		registerEvents(new ParcelLockerDropListener());
	}

	/*
	 * Plugin start/reload method
	 */

	@Override
	protected void onReloadablesStart() {

	}

	@Override
	public int getFoundedYear() {
		return 2022;
	}


	//* Instance getter
	public static ParcelLockersPlugin getInstance() {
		return (ParcelLockersPlugin) SimplePlugin.getInstance();
	}
}
