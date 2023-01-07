package xyz.jakubk15.parcellockers;

import lombok.Getter;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.plugin.SimplePlugin;
import xyz.jakubk15.parcellockers.command.ParcelCommand;
import xyz.jakubk15.parcellockers.listener.ParcelLockerDropListener;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;
import xyz.jakubk15.parcellockers.task.CancelledParcelClearTask;

import java.util.*;

public class ParcelLockersPlugin extends SimplePlugin {

	public ParcelLockersPlugin() {
	}

	@Getter
	private Map<ParcelLocker, List<Parcel>> parcelDatabase = new HashMap<>();
	@Getter
	private Set<Parcel> cancelledParcels = new HashSet<>();

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		registerCommand(new ParcelCommand());
		registerEvents(new ParcelLockerDropListener());
		new CancelledParcelClearTask().runTaskTimer(this, 0, 20 * 60 * 360);
	}

	@Override
	protected void onReloadablesStart() {
		this.onPluginStart();
	}

	@Override
	public int getFoundedYear() {
		return 2022;
	}

	@Override
	public MinecraftVersion.V getMinimumVersion() {
		return MinecraftVersion.V.v1_8;
	}

	@Override
	public MinecraftVersion.V getMaximumVersion() {
		return MinecraftVersion.V.v1_19;
	}

	@Override
	public boolean suggestPaper() {
		return true;
	}

	@Override
	protected String[] getStartupLogo() {
		return new String[] {
			"  ____                    _ _               _                 ",
			" |  _ \\ __ _ _ __ ___ ___| | |    ___   ___| | _____ _ __ ___ ",
			" | |_) / _` | '__/ __/ _ \\ | |   / _ \\ / __| |/ / _ \\ '__/ __|",
			" |  __/ (_| | | | (_|  __/ | |__| (_) | (__|   <  __/ |  \\__ \\",
			" |_|   \\__,_|_|  \\___\\___|_|_____\\___/ \\___|_|\\_\\___|_|  |___/",
			"                                                              "
		};
	}

	public static ParcelLockersPlugin getInstance() {
		return (ParcelLockersPlugin) SimplePlugin.getInstance();
	}

}
