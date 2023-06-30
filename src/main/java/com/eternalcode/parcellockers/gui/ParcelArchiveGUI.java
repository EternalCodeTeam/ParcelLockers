package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

// TODO
public class ParcelArchiveGUI implements View {

    private static final int[] CORNER_SLOTS = {0, 8, 45, 53};
    private static final int[] BORDER_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;

    public ParcelArchiveGUI(MiniMessage miniMessage, PluginConfiguration config, ParcelRepository parcelRepository) {
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
    }

    @Override
    public void show(Player player) {

    }
}
