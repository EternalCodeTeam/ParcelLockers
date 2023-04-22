package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.ParcelRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ParcelArchiveGUI {

    private final int[] cornerSlots = {0, 8, 45, 53};
    private final int[] borderSlots = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository repository;

    public ParcelArchiveGUI(MiniMessage miniMessage, PluginConfiguration config, ParcelRepository repository) {
        this.miniMessage = miniMessage;
        this.config = config;
        this.repository = repository;
    }

}
