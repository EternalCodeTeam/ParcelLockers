package xyz.jakubk15.parcellockers;

import org.mineacademy.fo.settings.SimpleSettings;

/*
 * Plugin's settings class
 */

public class Settings extends SimpleSettings {

	private static void init() {
		setPathPrefix(null);

	}

	@Override
	protected int getConfigVersion() {
		return 1;
	}
}
