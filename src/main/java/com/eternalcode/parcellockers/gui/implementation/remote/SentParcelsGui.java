package com.eternalcode.parcellockers.gui.implementation.remote;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.parcellockers.configuration.implementation.PluginConfig.GuiSettings;
import com.eternalcode.parcellockers.configuration.serializable.ConfigItem;
import com.eternalcode.parcellockers.gui.GuiManager;
import com.eternalcode.parcellockers.gui.GuiView;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.util.PlaceholderUtil;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@SuppressWarnings({"Convert2MethodRef", "ClassCanBeRecord"})
public class SentParcelsGui implements GuiView {

    private final Scheduler scheduler;
    private final MiniMessage miniMessage;
    private final GuiSettings guiSettings;
    private final MainGui mainGUI;
    private final GuiManager guiManager;

    public SentParcelsGui(
        Scheduler scheduler,
        MiniMessage miniMessage,
        GuiSettings guiSettings,
        MainGui mainGUI,
        GuiManager guiManager
    ) {
        this.scheduler = scheduler;
        this.miniMessage = miniMessage;
        this.guiSettings = guiSettings;
        this.mainGUI = mainGUI;
        this.guiManager = guiManager;
    }

    @Override
    public void show(Player player) {
        PaginatedGui gui = Gui.paginated()
            .title(this.miniMessage.deserialize(this.guiSettings.sentParcelsTitle))
            .rows(6)
            .disableAllInteractions()
            .create();

        ConfigItem parcelItem = this.guiSettings.parcelItem;
        GuiItem cornerItem = this.guiSettings.cornerItem.toGuiItem();
        GuiItem backgroundItem = this.guiSettings.mainGuiBackgroundItem.toGuiItem();
        GuiItem closeItem = this.guiSettings.closeItem.toGuiItem(event -> this.mainGUI.show(player));

        for (int slot : CORNER_SLOTS) {
            gui.setItem(slot, cornerItem);
        }

        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, backgroundItem);
        }

        gui.setItem(49, closeItem);


        this.guiManager.getParcelBySender(player.getUniqueId()).thenAccept(optionalParcels -> {
            List<Parcel> parcels = optionalParcels.orElse(Collections.emptyList());

            for (Parcel parcel : parcels) {
                PaperItemBuilder item = parcelItem.toBuilder();

                List<Component> newLore = PlaceholderUtil.replaceParcelPlaceholders(parcel, parcelItem.lore(), this.guiManager)
                    .stream()
                    .map(line -> this.miniMessage.deserialize(line))
                    .toList();
                item.lore(newLore);
                item.name(this.miniMessage.deserialize(parcelItem.name()));

                gui.addItem(item.asGuiItem());
            }
            this.scheduler.run(() -> gui.open(player));
        });
    }
}
