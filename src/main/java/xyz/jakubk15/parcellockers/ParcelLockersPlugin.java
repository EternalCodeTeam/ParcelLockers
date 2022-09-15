package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.jakubk15.parcellockers.command.ParcelCommand;
import xyz.jakubk15.parcellockers.listener.ParcelLockerDropListener;
import xyz.jakubk15.parcellockers.model.Parcel;

import java.util.HashSet;
import java.util.Set;

public class ParcelLockersPlugin extends SimplePlugin {

	public Set<Parcel> parcelSet = new HashSet<>();

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		registerCommand(new ParcelCommand());
		registerEvents(new ParcelLockerDropListener());
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
