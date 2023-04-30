package com.eternalcode.parcellockers.gui;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.ParcelMeta;
import com.eternalcode.parcellockers.parcel.ParcelRepository;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

public class SentParcelsGUI {

    private static final int[] CORNER_SLOTS = {0, 8, 45, 53};
    private static final int[] BORDER_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};
    private final Server server;
    private final MiniMessage miniMessage;
    private final PluginConfiguration config;
    private final ParcelRepository repository;

    public SentParcelsGUI(Server server, MiniMessage miniMessage, PluginConfiguration config, ParcelRepository repository) {
        this.server = server;
        this.miniMessage = miniMessage;
        this.config = config;
        this.repository = repository;
    }

    public void showSentParcelsGUI(Player player) {

        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem(this.miniMessage);
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem(this.miniMessage);
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(this.miniMessage);
        PaginatedGui gui = Gui.paginated()
                .title(this.miniMessage.deserialize(this.config.guiSettings.sentParcelsTitle))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        for (Parcel parcel : this.repository.findBySender(player.getUniqueId()).join()) {
            ParcelMeta meta = parcel.meta();
            String sender = player.getName();
            String receiver = Bukkit.getPlayer(meta.getReceiver()).getName();

            gui.addItem(ItemBuilder.from(Material.BUNDLE)
                    .name(this.miniMessage.deserialize(meta.getName()))
                    .lore(this.miniMessage.deserialize("&3UUID: ").append(Component.text(parcel.uuid().toString())),
                            this.miniMessage.deserialize("&3Sender: ").append(Component.text(sender)),
                            this.miniMessage.deserialize("&3Receiver: ").append(Component.text(receiver)),
                            this.miniMessage.deserialize("&3Size: ").append(Component.text(meta.getSize().toString())),
                            this.miniMessage.deserialize("&3Priority: ").append(Component.text(meta.isPriority() ? "&aYes" : "&cNo")),
                            this.miniMessage.deserialize("&3Description: ").append(Component.text(meta.getDescription())),
                            this.miniMessage.deserialize("&3Entry position: ").append(Component.text(meta.getEntryLocker().getPosition().toString())),
                            this.miniMessage.deserialize("&3Target Position: ").append(Component.text(meta.getDestinationLocker().getPosition().toString())),
                            this.miniMessage.deserialize("&3Recipients: ").append(Component.text(meta.getRecipients().stream().map(Bukkit::getPlayer).map(HumanEntity::getName).toList().toString())))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .asGuiItem());
        }
    }
}
