package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

public class ParcelLockers extends SimplePlugin {

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
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
