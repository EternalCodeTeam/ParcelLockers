package xyz.jakubk15.parcellockers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.plugin.SimplePlugin;
import panda.std.stream.PandaStream;
import xyz.jakubk15.parcellockers.command.ParcelCommand;
import xyz.jakubk15.parcellockers.listener.ParcelLockerBreakListener;
import xyz.jakubk15.parcellockers.listener.ParcelLockerDropListener;
import xyz.jakubk15.parcellockers.listener.ParcelLockerPlaceListener;
import xyz.jakubk15.parcellockers.model.Parcel;
import xyz.jakubk15.parcellockers.model.ParcelLocker;
import xyz.jakubk15.parcellockers.task.CancelledParcelClearTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class ParcelLockersPlugin extends SimplePlugin {

	@Getter
	private final Map<ParcelLocker, List<Parcel>> parcelDatabase = new HashMap<>();
	@Getter
	private final Set<Parcel> cancelledParcels = new HashSet<>();

	@Override
	protected void onPluginStart() {
		Common.setLogPrefix("ParcelLockers");
		long started = System.currentTimeMillis();

		PandaStream.of(
			new ParcelCommand()
		).forEach(this::registerCommand);

		PandaStream.of(
			new ParcelLockerPlaceListener(this),
			new ParcelLockerBreakListener(this),
			new ParcelLockerDropListener()
		).forEach(this::registerEvents);

		new CancelledParcelClearTask().run();
		Common.log("Plugin enabled in " + (System.currentTimeMillis() - started) + "ms");
	}

	@Override
	protected void onPluginPreReload() {
		Bukkit.getLogger().severe(Common.chatLineSmooth());
		Bukkit.getLogger().severe("ParcelLockers has been reloaded >:(");
		Bukkit.getLogger().severe("Please avoid reloading the plugin, it may cause unexpected behaviour, and you will not receive any support from us");
		Bukkit.getLogger().severe("If you need to reload the plugin, please restart the server, or use the built-in reload command.");
		Bukkit.getLogger().severe(Common.chatLineSmooth());
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
