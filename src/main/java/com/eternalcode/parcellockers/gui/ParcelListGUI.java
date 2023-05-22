package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelMeta;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParcelListGUI {

    private static final int[] CORNER_SLOTS = {0, 8, 45, 53};
    private static final int[] BORDER_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};

    private final Plugin plugin;
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository parcelRepository;


    public ParcelListGUI(Plugin plugin, Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepository parcelRepository) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
    }

    public void showParcelListGUI(Player player) {

        GuiItem parcelItem = this.config.guiSettings.parcelItem.toGuiItem(this.miniMessage);
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage);

        PaginatedGui gui = Gui.paginated()
                .title(this.miniMessage.deserialize(this.config.guiSettings.parcelListGuiTitle))
                .disableAllInteractions()
                .rows(6)
                .create();


        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        this.parcelRepository.findAll().whenComplete((parcels, throwable) -> {
            for (Parcel parcel : parcels) {
                List<String> newLore = this.replaceParcelPlaceholders(parcel, parcelItem.getItemStack().getItemMeta().getLore());
                parcelItem.getItemStack().getItemMeta().setLore(newLore);

                gui.addItem(parcelItem);
            }

            gui.setItem(49, closeItem);
            this.server.getScheduler().runTask(this.plugin, () -> gui.open(player));
        });

    }

    public List<String> replaceParcelPlaceholders(Parcel parcel, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }

        ParcelMeta meta = parcel.meta();
        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            line = line.replace("{UUID}", parcel.uuid().toString());
            line = line.replace("{NAME}", meta.getName());
            line = line.replace("{SENDER}", this.server.getPlayer(parcel.sender()).getName());
            line = line.replace("{RECEIVER}", this.server.getPlayer(meta.getReceiver()).getName());
            line = line.replace("{SIZE}", meta.getSize().toString());
            line = line.replace("{PRIORITY}", meta.isPriority() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "&cNo");
            line = line.replace("{DESCRIPTION}", meta.getDescription());
            line = line.replace("{POSITION}",
                    "X: " + meta.getDestinationLocker().getPosition().x()
                            + " Y: " + meta.getDestinationLocker().getPosition().y()
                            + " Z: " + meta.getDestinationLocker().getPosition().z());
            line = line.replace("{RECIPIENTS}", meta.getRecipients().stream()
                    .map(Bukkit::getPlayer)
                    .map(HumanEntity::getName)
                    .toList()
                    .toString());
            newLore.add(line);
        }
        return newLore;
    }

}
