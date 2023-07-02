package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

// TODO archive gui
public class ParcelArchiveGUI extends GuiView {

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
