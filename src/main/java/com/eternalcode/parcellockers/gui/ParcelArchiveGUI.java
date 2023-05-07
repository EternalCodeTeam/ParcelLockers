package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.database.ParcelDatabaseService;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ParcelArchiveGUI {

    private static final int[] CORNER_SLOTS = {0, 8, 45, 53};
    private static final int[] BORDER_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelDatabaseService parcelDatabaseService;

    public ParcelArchiveGUI(MiniMessage miniMessage, PluginConfiguration config, ParcelDatabaseService parcelDatabaseService) {
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelDatabaseService = parcelDatabaseService;
    }

}
