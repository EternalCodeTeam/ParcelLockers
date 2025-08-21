package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.locker.repository.LockerRepository;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.repository.ParcelRepository;
import com.eternalcode.parcellockers.parcel.util.ParcelPlaceholderUtil;
import com.eternalcode.parcellockers.user.UserManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SentParcelsGui implements GuiView {

    private final Plugin plugin;
    private final MiniMessage miniMessage;
    private final PluginConfig config;
    private final ParcelRepository parcelRepository;
    private final LockerRepository lockerRepository;
    private final MainGui mainGUI;
    private final UserManager userManager;

    public SentParcelsGui(
        Plugin plugin,
        MiniMessage miniMessage,
        PluginConfig config,
        ParcelRepository parcelRepository,
        LockerRepository lockerRepository,
        MainGui mainGUI,
        UserManager userManager
    ) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
        this.config = config;
        this.parcelRepository = parcelRepository;
        this.lockerRepository = lockerRepository;
        this.mainGUI = mainGUI;
        this.userManager = userManager;
    }

    @Override
    public void show(Player player) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.config.guiSettings.sentParcelsTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        ConfigItem parcelItem = this.config.guiSettings.parcelItem;
        GuiItem cornerItem = this.config.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.config.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.config.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);


        this.parcelRepository.findBySender(player.getUniqueId()).thenAccept(optionalParcels -> {
            List<Parcel> parcels = optionalParcels.orElse(Collections.emptyList());

            for (Parcel parcel : parcels) {
                ItemBuilder item = parcelItem.toBuilder();

                List<Component> newLore = ParcelPlaceholderUtil.replaceParcelPlaceholders(parcel, parcelItem.lore, this.userManager, this.lockerRepository)
                    .stream()
                    .map(line -> this.miniMessage.deserialize(line))
                    .toList();
                item.lore(newLore);
                item.name(this.miniMessage.deserialize(parcelItem.name.replace("{NAME}", parcel.name())));

                gui.addItem(item.asGuiItem());
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> gui.open(player));
        });
    }
}
